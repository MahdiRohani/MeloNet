#!/usr/bin/env bash
# Prints the best LAN IPv4 for reaching this machine from a phone / emulator
# on the same network. Prefers Wi‑Fi/Ethernet, skips docker/vpn/bridge NICs.
set -euo pipefail

is_usable_iface() {
  local name="$1"
  case "$name" in
    lo|docker*|br-*|veth*|tun*|tap*|virbr*|vmnet*|incus*|lxd*|cni*|flannel*|weave*|zt*|tailscale*|wg*)
      return 1
      ;;
  esac
  return 0
}

is_preferred_iface() {
  local name="$1"
  case "$name" in
    wl*|wlan*|en*|eth*|wlp*|enp*|eno*|ens*)
      return 0
      ;;
  esac
  return 1
}

# RFC1918 private ranges only (reachable as a typical home/office LAN).
is_lan_ipv4() {
  local ip="$1"
  case "$ip" in
    10.*|192.168.*|172.1[6-9].*|172.2[0-9].*|172.3[0-1].*)
      return 0
      ;;
  esac
  return 1
}

preferred=()
others=()

if command -v ip >/dev/null 2>&1; then
  while read -r ip name; do
    [[ -z "${ip:-}" || -z "${name:-}" ]] && continue
    is_usable_iface "$name" || continue
    is_lan_ipv4 "$ip" || continue
    if is_preferred_iface "$name"; then
      preferred+=("$ip")
    else
      others+=("$ip")
    fi
  done < <(ip -4 -o addr show scope global 2>/dev/null | awk '{gsub(/\/.*/, "", $4); print $4, $2}')
fi

if [[ ${#preferred[@]} -gt 0 ]]; then
  echo "${preferred[0]}"
  exit 0
fi
if [[ ${#others[@]} -gt 0 ]]; then
  echo "${others[0]}"
  exit 0
fi

# Fallback: hostname -I (first private-ish address).
if command -v hostname >/dev/null 2>&1; then
  for ip in $(hostname -I 2>/dev/null); do
    case "$ip" in
      *:*) continue ;; # skip IPv6
    esac
    is_lan_ipv4 "$ip" || continue
    echo "$ip"
    exit 0
  done
fi

echo "127.0.0.1"
exit 0
