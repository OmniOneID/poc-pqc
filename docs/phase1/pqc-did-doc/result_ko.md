# DID Document Comparison: Secp256r1 vs ML-DSA-44

## 분석

| 항목 | Secp256r1 | ML-DSA-44 | 비율 | 비고 |
|------|-----------|-----------|------|------|
| Public Key Size | 33 B | 1,334 B | **40.4x** | Multibase 디코딩 후 순수 바이트 |
| DID Doc Size | 630 B | 4,098 B | **6.5x** | 공개키 2개 포함 시 약 6.5배 |
| Key Gen (x2) | 331.1 ms | 46.4 ms | **0.1x** | ML-DSA-44가 약 7배 빠름 |

---

## 실행 결과

### Comparison Table

```
╔═══════════════════╤═══════════════╤═══════════════╤══════════╗
║      Comparison: Secp256r1 vs ML-DSA-44 (DID Document)       ║
╠═══════════════════╪═══════════════╪═══════════════╪══════════╣
║ Item              │     Secp256r1 │     ML-DSA-44 │    Ratio ║
╠═══════════════════╪═══════════════╪═══════════════╪══════════╣
║ Public Key Size   │          33 B │       1,334 B │   40.4x  ║
║ DID Doc Size      │         630 B │       4,098 B │    6.5x  ║
║ Key Gen (x2)      │    328.362 ms │     43.936 ms │    0.1x  ║
╚═══════════════════╧═══════════════╧═══════════════╧══════════╝
```

### Secp256r1 DID Document

```json
{
  "@context": ["https://www.w3.org/ns/did/v1"],
  "assertionMethod": ["assert"],
  "authentication": ["auth"],
  "controller": "did:omn:tas",
  "deactivated": false,
  "id": "did:omn:issuer",
  "verificationMethod": [
    {
      "authType": 1,
      "controller": "did:omn:tas",
      "id": "assert",
      "publicKeyMultibase": "z28L1Y6VUqMzKEQ5dc1cPLR3azwXEkHs1UYW79zAPB1zdv",
      "type": "Secp256r1VerificationKey2018"
    },
    {
      "authType": 1,
      "controller": "did:omn:tas",
      "id": "auth",
      "publicKeyMultibase": "zmMkTQeDBEiRwd9pmuWUZdBb4GJQZmNTZWpQHyei4LKKq",
      "type": "Secp256r1VerificationKey2018"
    }
  ]
}
```

### ML-DSA-44 DID Document

```json
{
  "@context": ["https://www.w3.org/ns/did/v1"],
  "assertionMethod": ["assert"],
  "authentication": ["auth"],
  "controller": "did:omn:tas",
  "deactivated": false,
  "id": "did:omn:issuer",
  "verificationMethod": [
    {
      "authType": 1,
      "controller": "did:omn:tas",
      "id": "assert",
      "publicKeyMultibase": "mMIIFMjALBglghkgBZQMEAxEDggUhAOcdhRIEp5RsDsZ1GuixhdOUpEi/LvxTZU3z6Bfs6KFx8WFcT3j6WONkqAGLiwoLdFv35lIYw+yG1vQOwZStaiW18BGjDhM69fNSnow3i+RciWWROlKqBzTXvpQyRucXSCKfn+EeRgiRjmE8wW46jyAIfJHGfUwzWBxACQqsnYLMHhDUYgv3P3ljGFRPab7t1vFNvezvYYM+LwxGuHh6+bryIxGaWd+6JNpUZ+n4JH7PfScDcc/J16eJgNeIc7Vm0CLJ8vuTcPaaIW8Jv4ymALKVMrGDJO8HewCY+GqkWyEUJT54x7QPotSvjvs4eyfHgoCzdtIN1J5sZL3ig8qPx3HmqgUCAHAVWBo03o6N5pe9PBUeRBC9SBg4Cfiali/HVoe1fg3Q3P4a/urgN2chO6R/zaK0CXX/iQf7XXpo4b0RTKRTyhta9NyArpHAJAKVbEmXTZYhFvKhH296NMFC0Jksl7vK89AfxVAOKThVW3sgyg0XIjmId7OoUPv+5iecmdJWmx8QQJkhV7VYBh6PN1miuHIfJRPNNdR6NRHmGrBKoHhpevjO1LLSasaraaqlFXEZZkldk8qIhSMXhF7bXlKh+t63CSTkH/ABrRLnKdBrorloHrENg/S8JV5+q4ujAk2CbnVkw47UU+hCJuq6CDY8wMkZHcvqWtcBKWmOG53WdJYrOwvxtKlszICzMt9HLe6JrCeclYREnowQXao8s90fItl2E1/98Jd/uZWeuWvZbWLjwSxdVU4lBQweA4ynCxKyiwhh+EtJcTHN9Rov7MKJFP2fgV6uFmE0PHh1TU7sU6nh3FiiXieKzJR/ScQbHMuuIlrHGqmNIxul08ZVjdnJXkjOxQz3/GoRb6BE1s9sMFC9Jqa6ECg1xkBBGALGpfR4k2uManO5bXg9Z2iUUETmQ0Wk/e2FJ+pMgPz9E5eqbaxT+pwkVERQ4eII8hNgB5IUZ3zmeoO0oaBNZnknHkk6GClSGp3XRF3NnUglBtLIy2/YCUZbOSNLvmWDP0NmzKgrzm77hto/L5kcpidUJt705PlNsk5RpxF9p1M4qzkDvLv9yPR5tM1fvF8ZSH8JyumqFI8DHI2UUXg9Ys4ws6e1WJpwZmyXBhYexXlHP9WCm2qLcto+t5W7O8dvobdT8W2ymvUxCz/S49DEGltNkCv2duErn9Zc73mDzn5wbiHIroHFrGQhT8/62Gp9qsiOFA/BjBwrcSddxBykAXzUyrcLuSrdXCRL5pe7vXqkNHvqzPUcfVh3VAFKjt7mWhzZn03JITkJKjQd6RKbKwGD84wHLmYXTPvCWx/c+u2qeNU0F9bxsxqAUkIs3Kyq4AdZg11huJHKrkJVXJdvVRG+9nUdBRftq03Kfu7o1Gz5TuNveZjrIK28FpY56bfK0MeCj17fbaD74DpzIM6QyGsvW3Ara7rJy6Cs/tcZpLbPrA71WjgZjU0WdTtwoRbAMDJK2AcYONhBCrK+omRemx2hd21eTQiZLLx2dySyRog+xUQ7bLgQLYVgor3Y2ZGoSUgXO9D9PBm0RRBjXUM2IvJ/GiVkMLVyvDL3kd6xx9sYc3zSYuBAQ03upipPLfckFN0gfiLnHqow4JCC+T9rZ/HkBVh4GBHN3BSIFjjW1pxSYBOvy7EmFaFKdDnUr6WySxY4zq/AOXjmZ9pCwv9sFerjsOEyQ14hO0eIg5syZvO9s39gtfXUUfOxoPfGyHx88s7csi/XwGHZwvzxHC5oBmF7pQ4=",
      "type": "MlDsa44VerificationKey2024"
    },
    {
      "authType": 1,
      "controller": "did:omn:tas",
      "id": "auth",
      "publicKeyMultibase": "mMIIFMjALBglghkgBZQMEAxEDggUhAKCFS8RDJ/Dd7/s92586JUa7BOtR/AvOHyAPOg1Sx8BnBB061WQ+/y1ECPAnVCnC0hwCzjZMC2DTBb+nIusxtA0pWkMtLYj8swl8UDn73DgQAiAVBRoEInImLlmwLgCWGdddh+Ur0eWZqL6DqD/JB4tQOO3If1a/4umAe+Ujwl8qj9IcrU1kE8xPPetWN985bLRVPGfs6ff8p86aB8ut7VbwLWqGigx9PajijwlenCxW9eHyIOq5VC71q2yrLZLS4DdnC70dDXgekmh6nAYcxVV5ZmiwUwdHyo7vrmRp5JiaJD5eHZ25M4RtiQfNHrVFAccjLdCUkmhTGFiLPEGiqcNHCDPcT2guw9n9ExfjNju3o9O2ABjaw8RLu1oesmm1NR1xveX9FcVEhXFZj+ETWZcm/woVnJccxrBgpymAQulXXvF6d+Ri3b4NVzkt1h7usj+ItB63AV9ifthuYQnBvi9p1ppWZHJalUrxGTiaBnv3c/zYXb11W7+UZnyTo1IPmMUbxhjIzZ6GppcY08t2V8Mu7xCOZMqwBxB+83OOnE704NozPm5bPQhqO33djo58uVRk5bTaglGub/JVdGfez6qIFFJaFFPWwmtCG10QyZNqjbwvA742pgaoTWSexeWJ7RFaosEKLaoYZOVz82ZROXDPzqGtPH0zdpUQR56UHK1FLxTuBtK93+B7E3oll8QTCR1qPkZJfNXOlKfQL1mIAagwgF4NNT4EVVhClabICKurtae9rjqWhpwmnYogaddDMIzIxE4v0wWZCLF+dN2LegMmYhIOdhGof6J6pVIEHwMSSQ5b72U6bdseDDSIIpYFZQtncMLVGgladdPhFsfvWpofgAJql9KoUjIG4zBYaB/p4RQyEEd/f689v/rexILtaXwj0YEUSkGhupApTJdTT4K077S6FZ0+q8of/Et+s6OSLu0bvFnGzKG+HT9jA3+POigE1z4A4EJe6NKChoS9xDRhB4W71VY80t60xc56aM7gREt/O2snxOiYBBkTTSZv5eqza4QNCfzvlykhwmTjk/Z6S/juDLk6utj7RUUE/Wb9bqM3tzWlf+B5LaaIs8CcOwsmKwgj47AqgfDfALBejEtrSGGNz7arIv1VIsG+0AQfXRM4OYfr7RsYIciIpEzaT/blAdRhHUaBeyWsjHS9uZ8/fAaIBODsYKZBTYMDfVXLpJfv8MWL7sHYessSSnBqIWX6LGjtNWMpZ41vWHOVU+Oo9mTnE8vfVJsRW9S9bha+6FA3tw5fVmoJh0TXx/VKrnTrpXYa2eIEJkrojYkJDKZvG8bpZXOM383EGrnF6J/XorDRF16479pqsnqAJwgwayaYtXGPOOB0ZW58amooWfXpAuRUUDlNPrmUC3EssKRyR/TVh42o0gg3rFE0t96hEsRPpB5+hsmuGboPOY23RDQDSe0BrQ5OCkkAw0RStyE9HeRH+9+DF0yf4aTwF6DeQAro3pZ2iOphKvsZ4LMZ1TNL/8/uldYtHra7x2HoPvvrst/k9jVv4ys5Pb4Cdt0GUVLJeAbrnjD1MewznQcPmNPTXR/8s9qNNiTPfRAI1c1St5Y6nDhq5DV3DHBMGfZoHVNsgjPZ+ije/pbFM2Lzod7yFuB+9yvo2BSFIZMLbSzVSilLbIcPnudxIUWs0Rq3XaEC3Or7zO9jNTCEIH12W8aWmfvKj03jkpMajJ1LSNNCJMdNS3aNzFqzfkdN03Go99x0b3YdK5EmQHBXnjgKzpA=",
      "type": "MlDsa44VerificationKey2024"
    }
  ]
}
```
