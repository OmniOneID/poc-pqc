# Key Agreement Comparison: ECDH(Secp256r1) vs ML-KEM-768

## 분석

| 항목 | ECDH | ML-KEM-768 | 비율 | 비고 |
|------|------|------------|------|------|
| Request Size | 501 B | 5,128 B | **10.2x** | ML-KEM 공개키 + ML-DSA 서명 포함 |
| Response Size | 402 B | 4,869 B | **12.1x** | ciphertext + ML-DSA 서명 포함 |
| Total Protocol | 180.3 ms | 69.8 ms | **0.4x** | ML-KEM이 약 2.6배 빠름 |

---

## 실행 결과

### Comparison Table

```
╔═══════════════════╤═══════════════╤═══════════════╤══════════╗
║          Comparison: ECDH(Secp256r1) vs ML-KEM-768           ║
╠═══════════════════╪═══════════════╪═══════════════╪══════════╣
║ Item              │          ECDH │    ML-KEM-768 │    Ratio ║
╠═══════════════════╪═══════════════╪═══════════════╪══════════╣
║ Request Size      │         501 B │       5,128 B │   10.2x  ║
║ Response Size     │         402 B │       4,869 B │   12.1x  ║
║ DID Setup         │    121.042 ms │     67.604 ms │    0.6x  ║
║ Step1 (Request)   │    147.980 ms │     29.482 ms │    0.2x  ║
║ Step2 (Response)  │     24.399 ms │     31.618 ms │    1.3x  ║
║ Step3 (Derive)    │      7.948 ms │      8.748 ms │    1.1x  ║
║ Total Protocol    │    180.327 ms │     69.848 ms │    0.4x  ║
╠═══════════════════╪═══════════════╪═══════════════╪══════════╣
║ Key Agreement     │         MATCH │         MATCH │          ║
╚═══════════════════╧═══════════════╧═══════════════╧══════════╝
```

### ECDH / Secp256r1

**reqECDH:**
```json
{
  "cipher": "AES-256-CBC",
  "curve": "Secp256r1",
  "nonce": "mnz0SJgdXtHrxyJ+UmahmHw==",
  "padding": "PKCS5",
  "proof": {
    "created": "2026-03-16T02:23:26.466239Z",
    "proofPurpose": "keyAgreement",
    "proofValue": "mH4un83xn5/WbB+SWyQYTuCPjiOk/FtU1MkkdPeo3IQfpQ48uU8fSZ6m8rGW8rKXkhzxYLAic6HwNfJdiNbXjGaw=",
    "type": "Secp256r1Signature2018",
    "verificationMethod": "did:omn:alice#keyagree"
  },
  "publicKey": "mMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKyRXsrlc3tFLAao9Y6wUdPUguk0Ff8H4FbdMvQBtmA5JPLC7HVCKZla0jdFk2MlGDMLa5rFgPoXSK16jkqqWfw=="
}
```

**resECDH:**
```json
{
  "proof": {
    "created": "2026-03-16T02:23:26.503645Z",
    "proofPurpose": "keyAgreement",
    "proofValue": "mIAoOpr8r5AVpkgC1x8TFkPt0ibehRe/4ZRGzaX12vnTkGuR4rYJ0nPXvdmSdGuOk3xqxieavYs7t0W7u2K8Lni8=",
    "type": "Secp256r1Signature2018",
    "verificationMethod": "did:omn:bob#keyagree"
  },
  "publicKey": "mMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEuKEPdFnuQC0QvdsutEEmTUhXyvgBCp8c8t+oHLOObj2znac/YbTFBb49RZ04Zn/FyBY02mJNjvdjSfrDcFibA=="
}
```

**Session Key:** `3093c898c5d56e93280b10f5fc201152a339972af6e68678c913376d416fcfa0`

### ML-KEM-768 / ML-DSA-44

**reqMLKEM:**
```json
{
  "algorithm": "ML-KEM-768",
  "cipher": "AES-256-CBC",
  "nonce": "mdUGrJF9GZclPdROj60VoSw==",
  "padding": "PKCS5",
  "proof": {
    "created": "2026-03-16T10:00:56.999941Z",
    "proofPurpose": "keyAgreement",
    "proofValue": "mrG0K1AgTD7DD9RbgS6ffkU47GwmJqsbfOO0PQAW9MbgexGonTiSec0qnDPn1P5BDFTr6e9am13gprLuXdkSBY7eJqMfbFVzupDAeOhe37IK3EpFoAeEL5qdBJFZCM7NaRC+KWAFcw2wQb4h89fNdWdjopPm2qb0Mo8HhK8RvPMzfpr6Jgthr6YRSqCM6sggbNJtm1+7ZGNl/29VTAOSfJUYr12IMWxAKxrMUpdlFZvqrLpTvdzMTtYt2hiPeeWSC+LPXyX6hUjp38JniYon3+o220n8STSB7OIdmzGAIGq5p1JdLkbxnodAFnqGwNWO7uEJVN6si3YFBD2WkDJHtK3QEwfkSxs+1KSCUO++e2wa+TGOcuA0eerHwCwOhGoyTS62XMU0mtFjYFY/UoDO7R5tKa1P25h3tx5tHsEhO0qFy0wYH4VNXthVkRON9jjyMaZtsM/yfuPcQkrb1vMGszGsZWbMvy592fvGk9KDG7MaEytRjFF+T/ZqFrlVR9t91paFFhyT+RlD/Djr2Xso+REN037mdyvWDnWFLaK5h01Vvq2SeDfQbtqgDFnKmA2jCltgJZ0YMKBuV7PcG/jdakPp5CwOYZfWeT31MrdeUQ9lEygtASxHRFzfDYOC9Uhta/kamJUdiFlz4td/3i9hOXTLoXjRtr/MqZk1iOVt7dwZLoFfiDq4CJBQI0SIJyZ9ChqsaTfo9uHLLJ1n21b//72hyu/dsuKzoRRzbSKgYzT9thSgJ4ziZLpNRj6EzoYrtnfrn3ulCqMZtpoLk31QS63RRy0sxVUnd5+i1IrjXZxHLRKc1Re5C6/Irw5QgbyMpJyxGaaO/8zDyN7X82qQMMMMSHNOvJGILPyhspmh6du4QzD+DQ7GES2zwfhhVFSbokvCGwwP8wlUdFMTsDxZIpaUHux7rbiMyTs6Lu87WFqJHtXPfoZINc0vg7LzNu4jvvImcFitRsON2u/nBvOhPwTX73TYMOsfgZPFU7WNMVtipuHRjimUSJQtG4whGttXrLQbt3BzZpYEJGPuxPV6vethlJyzO1sXOMLCUrZCbOiXbxUMMX01gajIqZ75/EDvgyJyA2nnn33qTuIMms6BIXFLRgzmme6cSZ5tvT4gvA1HnIUIxqSrSfI1YPCEBrzb6axoT97j8V2M0Ed978V+f172CKWvseWx/hIxmUxP/LnN2AsRB96hPaLW3pNayrfrqaq2jmIxABTaGPpacHPCYdodJPoiGFf/KfAtz4hltkVaZd8dxI7n1EB5P7pirIRAXkcfCycJfAoUbIHC75tKwHBfl0ozWODgfwKR53yuleHMmSqXjB6UBC253XbKo8nwFheM2jIZsXeZQVJkc+xl50C2QG1ljC/XTQCnZa0p5TR+H7t2bCfz4LGqzMGbXd/gB3I2xqxzQYDYHK4fNUfTxXPdI7zPUgP3SIlzhwpG+NKpzmIpNxZKI6t6I8b7rl1tPyc/MkuWL1Iu9a7XjYxBVIuJ258Y3xA/NwoLsJSfSiAuaC3GB86z9USBfl/8Finqz8yDLPa90IdMBSmt1P0jiY+1nCprZORzCOF0BpPepGl//KtG38D4DsfpZWJ2xfcjVSimeU5SD7DBrlG4efF3x8762QwpZbW3nAZd4L9/WmaMDWJ3T99Doh5jaXVcyynPBVDSIeeeAE2GQ12AebOorJnha6M2NOo1/KItjFR8/WyiB9oFu23TrqdkoYwVDVxcUTsLoAxVxc89M7Q9uKtlKTBtB34IfjQKkEOL9D6AfPJmU6CV0VuN+p/DVpmmgbqjqYWTDmeGJpNvGzRKROJgr2SgWGEGYpnVk8UUV/51TPGwUjIH2/P3V8orSZfKlFTHa/vv4l6z8G/RF9hSnWNTRcQeNIjda3SZA/quq1fQMALLJDfDE4g9FIH3l2+jV5wtf1v5hEQiKz5+c+8p+Ek1hCzvFuujsX+dN+E/gnCa+Pgul8yQFuVViKQOWCjztpc3Hh+8aAkhkeY/LbznFd8wYSXerxrSyeItGLG0eMPIgZwpOEguicJsmzNVUqqiRHvWS7hrXhmCNwkfEsah4hhBJtxsXRzlWCm7TX+GheOJLFNx4ZH0FkfTBMvw89EH84hCDNiaue7GM+Rs+Knij8VqHRKlrIyEc45sIgkIiz5nyEYYCOwB0uBllRIoDtk/O2haiSDtW9h47UiS9yVwD15oDPkLSFGUPTDvk6ddO8+bjWwTHEswV8PWVyoAmpP6XsJzXpjFqUk829OQhrgn2EYL8jKZLdLXonZLgoEqj4YpVA40Od/9j7j3XcUk7OBN7OXq+0A0G1MN3wWsyy2qO1gasrIhumZviKmgcXEz8QHgEIScOdRzQy5SWVoll7zrijx/rKfKfnPkpA2FPBycN+GcDpix+ROWiyHOw71foRqPNZ7zwNm8yzb6NLdYB5YRc6Azqf0OInAqVz5H4Pf0BPRC84nGXcneIYI8k0yIaErXCaKGScj/XRGL1lGIA0XXPdHUt2apoTaWW60ksITqxRvBiR+2rlez7mTc9vUuFUwGpucqEUpaW93EDZuKm12Ku6XpLfmhKKqNxS/yPujvl+srdwzhJog3P0ovwGA9p3hZp4FeYFsDhXZoFjssChELtTjl+9sPceVeK0hqtipgxG9OHLJmdZIgi05HZxTpV6JGqAUWTlxSbQhaGnTVYVRiVGbmwNEaSmGKOlBg5VT0RlNQuFwULt+jUGc+cG0pb2cXuYC4diug3TPmQxdHpDeXVW2uO97d7i/fmX8/3xOw5scJgJ4g0+cOoP6dmxwKwv/MdHpwSNuFjs1DUYqa5O6qgx3LdiCwkJUn5APUT9JsNlXr61g9uvzf4q7TqhJALpCHvqxvdnxNE2u+hWtXmWJurG5mh4LY32kJOp8kG02Yd5G6gm2p5JtmGhFx+vzYrBllXVQEuk+rnU33ziNFw/iKZ+wfH3G8qXtGd0uX6IfyKBnJlJ1Kz9pgs501trrtgIoysG5S1f3fLnONM/Tr5BhAD5B7C2SQauhxkvsNchAR3/Ld3JrxO1MIUDdsmkyeO68F6HJu7dfKNLo+eAYhRRGMFs8QlqEViW0fZT6HTVPF+FkyGuZ5FTz0xGz/NICYfq1ruHrYOGV9pe5Klwcff8QQKEhY6P0tSc42WotLi7O3+Bg8mOTpWb9Ll7/8CBxglL1NdZWl9gYiexfUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAscJzY=",
    "type": "MlDsa44Signature2024",
    "verificationMethod": "did:omn:alice#keyagree"
  },
  "publicKey": "mMIIEsjALBglghkgBZQMEBAIDggShAFr2w06FZNQ6FJLJKU9wABi7OZsWdIMpJECbKB6mWAiHX9uDavTxXYPcdu9bBHDnMCBEQNeTK17wlj1Bz3yKp3ACdT91DiHjAj/SivaKs+tQSmomOT6qJ1M7YVDzjYwpAKgXH2q1OWUaqii7nuJxo9PGthQbNRDSiCjUBQOgzLQ7GI5LcGBRYPOGah38NJVYaCnyl/ZqDWvhUGJZXKrzUGDXlgIavGPmei8xnrOifcUVdyRhsT1oWf1ETbQ7LwJTfCnlHhowCNd0WWIIjveZimpGefT4jGwYxA3XShU3kDHUBvG5lo8LdDORiLCpSG51JrYWpRtrj/LRaoU2DkdCc5tIuHZhERGsSvdkEoN1L90mtIgSTYwGGJlUIQHJPAIJ0Bl1ZUvlYCkVxC/ZpFcXk65lIP98kj63DRL1mUd1Lc35qFVotDbLIY2YXEshBrrSHhe5VI6jOhJUA6YQuD/FE+kzvYyCOaWqEQihtr4WUb/SE+SoqKu2nUj5dhW1BQOgTru0HsRDiIorMWA0d4sVTdnTbm18Ny5BG+YlnwI6O805TdeFk9ZSUGpbM0wiuwQRpc+6LKOEH31cRkmaJG0spHikGEd3vgnXXMYBEp4KTPxkBZkpSm+qWorFdKuJD4BwZ/njX25FtoLbLx9Ve1mSZ7pEPmuJvkE7SByFd2boTPWZD/NIAB96fexsQfdifZvMMPUzBpdrhZ7AYhErv4PkfkbqIAwsy1KBifKWCxbFkl36UdCSPsz6WAiSTprSQEbZqVzsCb2lNw8yQo4bZMyTuFyzEMNaz1X7AHFIfbTCGLb0X8sTRAXmAnCFo7fChgyUBz62E0/sj2EyZhf1BjZHof0WBYpDWTf3DiM3r+GpOWArxFOCLhc4d7RrYKBGetYabg+kvZVzaeyYFklLgPymPM0mlJH7nd3oGGyxjcJqnn6LYKihyve4g1tlbUd6PpDCl6Ewv35ify2wqVBjaGPoAjz3affMNe1CvxoBm4O7VJU2pdjLs3SJf+j1BjiqujDDHVfsBeh1kQixe+LSSVwZTGf0GoMUtns1xuKVlIc1l5JjjWiEQrEBeN7BNFWCV44In5PHYp52dR+KiPc0IBlTW73cfY45ebrUwm37U1xQGCqCJaKoJgSMn/S5t1ehUkfVVoACT0JYbQHCbg4wQ4+Uu2QDSuP0H6elxG+pXERnDpkQnMxRx5kLCWgcnmpUj4o8vPODBDsATeV7p8CDvDGKggZTKwPpWEKkTnzpxtgrJBhsOqnpdIVXz+w3eahnI1hWeMerM0SXfB1MzicqVNwEpWXsArPgu/e1I5B2A7x6fY9Ivj33krwxWes6vBcZI6EDQsx1ojtXfDMxGyfAIsjYFx0bVtOKfGhFUj6yc7MpHDslPnq7l43ptw+8fEyXENkMXI1zh2qGR1xYwlgmRXQXtkPToVTMKnogzb/7X6OZjhCXIqLYiiKMi/YrWMVZoSkFfkBAAfP8WKMnNwO7Ll2Cncq5y9q2Dwh1lukcsjHEZXT3JMv3GwJMrzm7pi3FgF4bZKlGU5x5BxH7tHrJcyCGCfKb4cupu8iLo5P9f2k2NrmZ"
}
```

**resMLKEM:**
```json
{
  "ciphertext": "mAiqijjDp3u1kFRmsK+bqUa248+4gmhqfxTjH5iaDOWuisvacdvuqk13Vq7tJoSsJnlVy4y6EiQXMV/SwYqkAXE2cEtTWnMK2dz74EytuNhtRvmj59N6o07/E5LVSKeYTzv1iewC+cB1MPlHfrgGwJ8hRFjNlMAKJpO8h6BjzgXZdRGmhUlJWY+RDunpPvdIWqGpL8BZZ3lQeTvCyKj3ExZnFIuSJk/PXJ86+Z4KqFGvV/DxrYzyvjRyWUtUiCJfQpkAmuyOB2h/3Z3MDSuU1ZJfc/J6vQc9bN/QXEKCG94Miqxoa1FBndbUb4MKHTQ3mKiAwBUzqTA6m+A7RnwdilG1gGGL4Y8y6CEQ/7UIAw0kcWuaEVxVXRzzHAx+IMCXFZB4CzhE/szkFr/Tnvg0EHt4wXTYOcMn5alifSenkQ9squProxXg+lPXLe/OnBW3v3iBEW78GEmbQLsoY1DuH1TuoiCgISvc/yKdDikrp292PZXYcSgZ5ZvTVHqCJIXjrhvuC2JhYXYK/ZizjZHWhgkuGo+fyKXZ0g9SmWz7PNow+nlZByCypd5jgIlQmYoEAt89uF2PAxJ1C+Xu9ZO6+gti1D43gL6S1Gn160ShmvgxZuKPNbi1eSo/zDT4sYJtHuI+AVe5L5KPKgttubbKnxLp4cHa/GpLBYLi4Xw1u4nQ5XzS8N/Kt+xEmsCUhhuX3GCG4E0kmLV9Yd3wCyg24pjsKPnINjw0FS/Xk1RjfiVcl9IPPyxkAAWm6TH5FdTwOJ5VrbYJcXQ0U0mlD5s6D0XiP/w9HG7o7XFmDZ2uXCIG+LaLQT8FfzQt5CD3JAb3V9kVrQn1OT3ZIUVJMkxwpeVPKdboQi3GyUEDJsOkVN7zn28Mzgyw60OXsCZcGGrM5HMmJWXKIU+OgMm+lOecjRXBxAMusuL5GaH8rucgQdIN/wC34deA+5g7QkkoL3vvS2uulBfZo6gd455hMunAop3hNQym1BwjXhln8gRVFwUggYU6yNdEO2kqM0ExsYzQqG1FIBHAuofZqWCxJzJnawoRar4llkcXmEEohMG3B4lcCQxVUNOG8bwK2nVWkwB6giCZLOGFF95lwXeO7OlK2kX/7LkCdJQt1czp32svVp0/u9JMyY9qjOor6+9pgme7ZiK4jULnA++MCUAwPC3rq6tCQ/eevIxgHvf1ve1Sn62yoxWZXyEHUmcqAeI+Lktg2S+CJ+z4DKrZmPbuqjt1AYRDcbEgMHd/cDIr4w8vqEcra/JotblgLgif9ghqCyRCTR8EvtEJSFiyCsO5Yt5LGRnqfxi1A/JI2jcLItnaHAF7wQsxuATV5TmQEKVIH0gI85GGFJu4lwxJA0ytoGGLRaHVoqj/eq979Q8kBpKp0Z3RqVMla+5o2KwhIjEK6l5+LyXKujSwB88tuwYwwr3Cry6K5uDJwnuoqjZJvq7b9Z54=",
  "proof": {
    "created": "2026-03-16T10:00:57.038094Z",
    "proofPurpose": "keyAgreement",
    "proofValue": "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",
    "type": "MlDsa44Signature2024",
    "verificationMethod": "did:omn:bob#keyagree"
  }
}
```

**Session Key:** `c5a8249c32deed8b692e525d8998f384cc76b63ffd5b8b1fa333ad4ebe15c6c1`
