package com.example.builder

import android.content.Context
import com.android.apksig.ApkSigner
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Collections
import java.util.Date

class ApkSigningEngine(private val context: Context) {

    fun signApk(inputApk: File, outputSignedApk: File) {
        val (privateKey, cert) = loadOrGenerateSigningKey()

        val signerConfig = ApkSigner.SignerConfig.Builder(
            "CERT",
            privateKey,
            Collections.singletonList(cert)
        ).build()

        val apkSigner = ApkSigner.Builder(Collections.singletonList(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputSignedApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()

        apkSigner.sign()

        if (!outputSignedApk.exists() || outputSignedApk.length() == 0L) {
            throw IllegalStateException("APK signing resulted in empty or missing file.")
        }
    }

    private fun loadOrGenerateSigningKey(): Pair<PrivateKey, X509Certificate> {
        // 1. Try loading bundled debug keystore from assets
        try {
            val keyStore = KeyStore.getInstance("PKCS12")
            context.assets.open("template_debug.keystore").use { stream ->
                keyStore.load(stream, "android".toCharArray())
            }
            val key = keyStore.getKey("androiddebugkey", "android".toCharArray()) as? PrivateKey
            val cert = keyStore.getCertificate("androiddebugkey") as? X509Certificate
            if (key != null && cert != null) {
                return Pair(key, cert)
            }
        } catch (e: Exception) {
            // Fallback to internal app keystore or generated key
        }

        // 2. Try loading internal keystore in app private storage
        val localKs = File(context.filesDir, "build_debug.keystore")
        if (localKs.exists()) {
            try {
                val keyStore = KeyStore.getInstance("PKCS12")
                FileInputStream(localKs).use { fis ->
                    keyStore.load(fis, "android".toCharArray())
                }
                val key = keyStore.getKey("androiddebugkey", "android".toCharArray()) as? PrivateKey
                val cert = keyStore.getCertificate("androiddebugkey") as? X509Certificate
                if (key != null && cert != null) {
                    return Pair(key, cert)
                }
            } catch (ignored: Exception) {}
        }

        // 3. Fallback: generate self-signed key in memory
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        // Generate self-signed cert
        val cert = generateSelfSignedCertificate(keyPair)
        return Pair(keyPair.private, cert)
    }

    private fun generateSelfSignedCertificate(keyPair: java.security.KeyPair): X509Certificate {
        // Use BouncyCastle bundled in apksig jar
        try {
            val builderClass = Class.forName("org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder")
            val signerClass = Class.forName("org.bouncycastle.operator.jcajce.JcaContentSignerBuilder")
            val converterClass = Class.forName("org.bouncycastle.cert.jcajce.JcaX509CertificateConverter")
            val x500NameClass = Class.forName("org.bouncycastle.asn1.x500.X500Name")

            val owner = x500NameClass.getConstructor(String::class.java).newInstance("CN=WebToAPK, O=WebToAPK, C=US")
            val serial = BigInteger.valueOf(System.currentTimeMillis())
            val notBefore = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
            val notAfter = Date(System.currentTimeMillis() + 25L * 365 * 24 * 60 * 60 * 1000L)

            val certBuilder = builderClass.getConstructor(
                x500NameClass, BigInteger::class.java, Date::class.java, Date::class.java,
                x500NameClass, java.security.PublicKey::class.java
            ).newInstance(owner, serial, notBefore, notAfter, owner, keyPair.public)

            val signerBuilder = signerClass.getConstructor(String::class.java).newInstance("SHA256withRSA")
            val buildMethod = signerClass.getMethod("build", java.security.PrivateKey::class.java)
            val signer = buildMethod.invoke(signerBuilder, keyPair.private)

            val certHolderMethod = builderClass.getMethod("build", Class.forName("org.bouncycastle.operator.ContentSigner"))
            val certHolder = certHolderMethod.invoke(certBuilder, signer)

            val converter = converterClass.getConstructor().newInstance()
            val getCertMethod = converterClass.getMethod("getCertificate", Class.forName("org.bouncycastle.cert.X509CertificateHolder"))
            return getCertMethod.invoke(converter, certHolder) as X509Certificate
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize signing certificate: ${e.message}", e)
        }
    }
}
