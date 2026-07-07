/*
 * Copyright 2024 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.crypto.keypair;

import java.security.PrivateKey;
import java.security.PublicKey;
import org.omnione.did.crypto.enums.MultiBaseType;
import org.omnione.did.crypto.exception.CryptoException;
import org.omnione.did.crypto.util.MultiBaseUtils;

// forten - PQC ML-DSA-44 KeyPair 구현체
public class MlDsaKeyPair implements KeyPairInterface {

  private PublicKey publicKey;
  private PrivateKey privateKey;

  public MlDsaKeyPair(PublicKey publicKey, PrivateKey privateKey) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  @Override
  public PublicKey getPublicKey() {
    return publicKey;
  }

  @Override
  public void setPublicKey(PublicKey publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  @Override
  public void setPrivateKey(PrivateKey privateKey) {
    this.privateKey = privateKey;
  }

  public String getBase58PubKey() throws CryptoException {
    return MultiBaseUtils.encode(publicKey.getEncoded(), MultiBaseType.base58btc);
  }
}
