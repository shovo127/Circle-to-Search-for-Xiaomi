import struct
import hashlib
from datetime import datetime

apk_path = r"D:\Debload Phone with a Single Command\NotiGuard-v2.1.apk"
with open(apk_path, "rb") as f:
    data = f.read()

sha256 = hashlib.sha256(data).hexdigest()
print(f"File: {apk_path}")
print(f"Size: {len(data)} bytes")
print(f"SHA-256: {sha256}")

magic = b"APK Sig Block 42"
idx = data.find(magic)
if idx != -1:
    print(f"\n[+] APK Signing Block found at offset {idx}")
    block_size = struct.unpack("<Q", data[idx-8:idx])[0]
    block_start = idx - block_size
    print(f"    Block size: {block_size} bytes, Start offset: {block_start}")
    
    offset = block_start + 8
    while offset < idx - 8:
        pair_len = struct.unpack("<Q", data[offset:offset+8])[0]
        pair_id = struct.unpack("<I", data[offset+8:offset+12])[0]
        id_str = f"0x{pair_id:08x}"
        scheme_name = {
            0x7109871a: "APK Signature Scheme v2",
            0xf05368c0: "APK Signature Scheme v3",
            0x1b93ad61: "APK Signature Scheme v3.1",
            0x42726577: "Padding block"
        }.get(pair_id, "Unknown Scheme ID")
        print(f"    - ID: {id_str} ({scheme_name}), Length: {pair_len} bytes")
        
        # If Scheme v2 or v3, inspect signer certificate
        if pair_id in (0x7109871a, 0xf05368c0):
            # Parse signers sequence
            signers_data = data[offset+12 : offset+8+pair_len]
            # First 4 bytes length of signers sequence
            if len(signers_data) > 4:
                signers_len = struct.unpack("<I", signers_data[:4])[0]
                print(f"      Signers sequence length: {signers_len} bytes")
        
        offset += 8 + pair_len
else:
    print("[-] No APK Signing Block Magic found")

