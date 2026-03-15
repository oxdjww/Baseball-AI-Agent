#!/usr/bin/env python3
"""
OCI ARM 인스턴스 자동 선점 스크립트
VM.Standard.A1.Flex (4 OCPU / 24GB RAM) — ap-chuncheon-1
"""

import os
import time
import sys
from pathlib import Path
from dotenv import load_dotenv

import oci
import requests

# ──────────────────────────────────────────
# 설정
# ──────────────────────────────────────────
SHAPE = "VM.Standard.A1.Flex"
OCPU = 4
MEM_GB = 24
REGION = "ap-chuncheon-1"
VCN_CIDR = "10.0.0.0/16"
SUBNET_CIDR = "10.0.0.0/24"
VCN_NAME = "kbaseball-vcn"
SUBNET_NAME = "kbaseball-subnet"
IGW_NAME = "kbaseball-igw"
RETRY_INTERVAL = 30  # seconds

INGRESS_PORTS = [22, 80, 443, 8080]

ENV_PATH = Path(__file__).parent / ".env"
load_dotenv(ENV_PATH)

TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
TELEGRAM_CHAT_ID = os.getenv("TELEGRAM_CHAT_ID")


# ──────────────────────────────────────────
# 유틸
# ──────────────────────────────────────────

def log(msg: str):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def send_telegram(text: str):
    if not TELEGRAM_BOT_TOKEN or not TELEGRAM_CHAT_ID:
        log("Telegram 환경변수 미설정 — 알림 생략")
        return
    url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendMessage"
    try:
        resp = requests.post(url, json={"chat_id": TELEGRAM_CHAT_ID, "text": text}, timeout=10)
        resp.raise_for_status()
        log("Telegram 알림 전송 완료")
    except Exception as e:
        log(f"Telegram 알림 실패: {e}")


def load_ssh_pubkey() -> str:
    for path in ["~/.ssh/id_ed25519.pub", "~/.ssh/id_rsa.pub"]:
        p = Path(path).expanduser()
        if p.exists():
            log(f"SSH 공개키 로드: {p}")
            return p.read_text().strip()
    raise FileNotFoundError("SSH 공개키를 찾을 수 없습니다 (~/.ssh/id_ed25519.pub 또는 id_rsa.pub)")


# ──────────────────────────────────────────
# 네트워크 설정 (멱등)
# ──────────────────────────────────────────

def get_or_create_vcn(vcn_client, compartment_id: str) -> oci.core.models.Vcn:
    existing = vcn_client.list_vcns(compartment_id, display_name=VCN_NAME).data
    if existing:
        log(f"기존 VCN 재사용: {existing[0].id}")
        return existing[0]

    log(f"VCN 생성 중: {VCN_NAME}")
    result = vcn_client.create_vcn(
        oci.core.models.CreateVcnDetails(
            cidr_block=VCN_CIDR,
            compartment_id=compartment_id,
            display_name=VCN_NAME,
        )
    )
    vcn = oci.wait_until(vcn_client, vcn_client.get_vcn(result.data.id), "lifecycle_state", "AVAILABLE").data
    log(f"VCN 생성 완료: {vcn.id}")
    return vcn


def ensure_internet_gateway(vcn_client, compartment_id: str, vcn: oci.core.models.Vcn):
    existing = vcn_client.list_internet_gateways(compartment_id, vcn_id=vcn.id, display_name=IGW_NAME).data
    if existing:
        igw = existing[0]
        log(f"기존 IGW 재사용: {igw.id}")
    else:
        log("Internet Gateway 생성 중")
        igw = vcn_client.create_internet_gateway(
            oci.core.models.CreateInternetGatewayDetails(
                compartment_id=compartment_id,
                vcn_id=vcn.id,
                is_enabled=True,
                display_name=IGW_NAME,
            )
        ).data
        log(f"IGW 생성 완료: {igw.id}")

    # Default Route Table 업데이트
    rt_list = vcn_client.list_route_tables(compartment_id, vcn_id=vcn.id).data
    default_rt = next((rt for rt in rt_list if rt.display_name == "Default Route Table for " + VCN_NAME), rt_list[0])

    already_routed = any(r.network_entity_id == igw.id for r in (default_rt.route_rules or []))
    if not already_routed:
        log("Default Route Table에 IGW 라우팅 추가")
        vcn_client.update_route_table(
            default_rt.id,
            oci.core.models.UpdateRouteTableDetails(
                route_rules=[
                    oci.core.models.RouteRule(
                        network_entity_id=igw.id,
                        destination="0.0.0.0/0",
                        destination_type="CIDR_BLOCK",
                    )
                ]
            ),
        )
    else:
        log("Route Table 이미 구성됨 — 건너뜀")

    return igw


def ensure_security_list(vcn_client, compartment_id: str, vcn: oci.core.models.Vcn):
    sl_list = vcn_client.list_security_lists(compartment_id, vcn_id=vcn.id).data
    default_sl = sl_list[0]

    existing_ports = {
        r.tcp_options.destination_port_range.min
        for r in (default_sl.ingress_security_rules or [])
        if r.tcp_options and r.tcp_options.destination_port_range
    }

    missing = [p for p in INGRESS_PORTS if p not in existing_ports]
    if not missing:
        log("Security List 이미 구성됨 — 건너뜀")
        return

    log(f"Security List ingress 규칙 추가: {missing}")
    new_rules = list(default_sl.ingress_security_rules or [])
    for port in missing:
        new_rules.append(
            oci.core.models.IngressSecurityRule(
                protocol="6",  # TCP
                source="0.0.0.0/0",
                source_type="CIDR_BLOCK",
                tcp_options=oci.core.models.TcpOptions(
                    destination_port_range=oci.core.models.PortRange(min=port, max=port)
                ),
            )
        )
    vcn_client.update_security_list(
        default_sl.id,
        oci.core.models.UpdateSecurityListDetails(ingress_security_rules=new_rules),
    )


def get_or_create_subnet(vcn_client, compartment_id: str, vcn: oci.core.models.Vcn) -> oci.core.models.Subnet:
    existing = vcn_client.list_subnets(compartment_id, vcn_id=vcn.id, display_name=SUBNET_NAME).data
    if existing:
        log(f"기존 Subnet 재사용: {existing[0].id}")
        return existing[0]

    log(f"Subnet 생성 중: {SUBNET_NAME}")
    result = vcn_client.create_subnet(
        oci.core.models.CreateSubnetDetails(
            compartment_id=compartment_id,
            vcn_id=vcn.id,
            cidr_block=SUBNET_CIDR,
            display_name=SUBNET_NAME,
        )
    )
    subnet = oci.wait_until(vcn_client, vcn_client.get_subnet(result.data.id), "lifecycle_state", "AVAILABLE").data
    log(f"Subnet 생성 완료: {subnet.id}")
    return subnet


def setup_network(config: dict) -> str:
    vcn_client = oci.core.VirtualNetworkClient(config)
    compartment_id = config["tenancy"]

    vcn = get_or_create_vcn(vcn_client, compartment_id)
    ensure_internet_gateway(vcn_client, compartment_id, vcn)
    ensure_security_list(vcn_client, compartment_id, vcn)
    subnet = get_or_create_subnet(vcn_client, compartment_id, vcn)

    return subnet.id


# ──────────────────────────────────────────
# 이미지 조회
# ──────────────────────────────────────────

def get_ubuntu_image(config: dict) -> str:
    compute_client = oci.core.ComputeClient(config)
    compartment_id = config["tenancy"]

    images = compute_client.list_images(
        compartment_id,
        operating_system="Canonical Ubuntu",
        operating_system_version="22.04",
        shape=SHAPE,
        sort_by="TIMECREATED",
        sort_order="DESC",
    ).data

    if not images:
        raise RuntimeError("Ubuntu 22.04 이미지를 찾을 수 없습니다")

    log(f"이미지 선택: {images[0].display_name} ({images[0].id})")
    return images[0].id


# ──────────────────────────────────────────
# 인스턴스 생성 루프
# ──────────────────────────────────────────

def get_public_ip(compute_client, network_client, instance_id: str) -> str | None:
    vnics = compute_client.list_vnic_attachments(
        compute_client.base_client.config["tenancy"], instance_id=instance_id
    ).data
    for vnic_attachment in vnics:
        vnic = network_client.get_vnic(vnic_attachment.vnic_id).data
        if vnic.public_ip:
            return vnic.public_ip
    return None


def launch_loop(config: dict, subnet_id: str, image_id: str, ssh_pubkey: str):
    compute_client = oci.core.ComputeClient(config)
    identity_client = oci.identity.IdentityClient(config)
    network_client = oci.core.VirtualNetworkClient(config)
    compartment_id = config["tenancy"]

    ads = identity_client.list_availability_domains(compartment_id).data
    log(f"가용 도메인: {[ad.name for ad in ads]}")

    attempt = 0
    while True:
        attempt += 1
        log(f"=== 시도 #{attempt} ===")

        for ad in ads:
            log(f"AD 시도: {ad.name}")
            try:
                instance = compute_client.launch_instance(
                    oci.core.models.LaunchInstanceDetails(
                        availability_domain=ad.name,
                        compartment_id=compartment_id,
                        shape=SHAPE,
                        shape_config=oci.core.models.LaunchInstanceShapeConfigDetails(
                            ocpus=OCPU,
                            memory_in_gbs=MEM_GB,
                        ),
                        subnet_id=subnet_id,
                        image_id=image_id,
                        display_name="kbaseball-arm",
                        metadata={"ssh_authorized_keys": ssh_pubkey},
                        create_vnic_details=oci.core.models.CreateVnicDetails(
                            assign_public_ip=True,
                            subnet_id=subnet_id,
                        ),
                    )
                ).data

                log(f"인스턴스 생성 요청 성공! ID: {instance.id}")
                log("RUNNING 상태 대기 중...")

                instance = oci.wait_until(
                    compute_client,
                    compute_client.get_instance(instance.id),
                    "lifecycle_state",
                    "RUNNING",
                    max_wait_seconds=300,
                ).data

                log("인스턴스 RUNNING 상태 확인!")
                time.sleep(10)  # VNIC attach 대기

                public_ip = get_public_ip(compute_client, network_client, instance.id)
                log(f"공인 IP: {public_ip}")

                message = (
                    f"✅ OCI ARM 인스턴스 선점 성공!\n\n"
                    f"인스턴스 ID: {instance.id}\n"
                    f"AD: {ad.name}\n"
                    f"Shape: {SHAPE} ({OCPU} OCPU / {MEM_GB}GB)\n"
                    f"공인 IP: {public_ip}\n\n"
                    f"SSH 접속:\n"
                    f"ssh ubuntu@{public_ip}"
                )
                print("\n" + "=" * 50)
                print(message)
                print("=" * 50 + "\n")
                send_telegram(message)
                return

            except oci.exceptions.ServiceError as e:
                if "Out of host capacity" in str(e.message) or "InternalError" in str(e.code) or "LimitExceeded" in str(e.message):
                    log(f"  → 용량 부족 ({ad.name}), 다음 AD 시도")
                elif e.status == 429:
                    log(f"  → TooManyRequests — {RETRY_INTERVAL}초 대기")
                    time.sleep(RETRY_INTERVAL)
                else:
                    log(f"  → ServiceError ({ad.name}): [{e.status}] {e.code} — {e.message}")
            except oci.exceptions.RequestException as e:
                log(f"  → 네트워크 오류 ({ad.name}), 재시도 대기: {type(e).__name__}")
                time.sleep(RETRY_INTERVAL)
            except Exception as e:
                log(f"  → 예상치 못한 오류 ({ad.name}): {type(e).__name__}: {e}")

        log(f"{RETRY_INTERVAL}초 후 재시도...")
        time.sleep(RETRY_INTERVAL)


# ──────────────────────────────────────────
# 메인
# ──────────────────────────────────────────

def main():
    log("OCI ARM 선점 스크립트 시작")

    # OCI 설정 로드
    config = oci.config.from_file()
    oci.config.validate_config(config)
    log(f"OCI 설정 로드 성공 — region: {config['region']}, tenancy: {config['tenancy'][:30]}...")

    # SSH 키 로드
    ssh_pubkey = load_ssh_pubkey()

    # 네트워크 설정
    log("── 네트워크 설정 시작 ──")
    subnet_id = setup_network(config)
    log(f"Subnet ID: {subnet_id}")

    # Ubuntu 이미지 조회
    log("── 이미지 조회 ──")
    image_id = get_ubuntu_image(config)

    # 인스턴스 생성 루프
    log("── 인스턴스 생성 루프 시작 ──")
    launch_loop(config, subnet_id, image_id, ssh_pubkey)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        log("\n사용자 중단 (Ctrl+C)")
        sys.exit(0)
    except Exception as e:
        log(f"치명적 오류: {e}")
        raise
