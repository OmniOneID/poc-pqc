/*
 * Decompiled with CFR 0.152.
 */
package org.omnione.did.poc.pqc.server.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import org.omnione.did.common.util.JsonUtil;
import org.omnione.did.common.vo.QrImageData;

public class QrMaker {
    private static final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();
    private static final String DEFAULT_IMAGE_FORMAT = "PNG";
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;

    public static QrImageData makeQrImage(Object data) throws IOException, WriterException {
        byte[] qrImage = QrMaker.makeQrImage(JsonUtil.serializeToJson(data), DEFAULT_IMAGE_FORMAT, 300, 300);
        QrImageData qrImageData = new QrImageData();
        qrImageData.setFormat("images");
        qrImageData.setMediaType(DEFAULT_IMAGE_FORMAT);
        qrImageData.setQrIamge(qrImage);
        return qrImageData;
    }

    public static byte[] makeQrImage(String contents, String format, int width, int height) throws IOException, WriterException {
        BitMatrix bitMatrix = QrMaker.makeQrBitMatrix(contents, width, height);
        try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();){
            MatrixToImageWriter.writeToStream(bitMatrix, format, pngOutputStream);
            byte[] byArray = pngOutputStream.toByteArray();
            return byArray;
        }
    }

    public static BitMatrix makeQrBitMatrix(String contents, int width, int height) throws WriterException {
        HashMap<EncodeHintType, Integer> hints = new HashMap<EncodeHintType, Integer>();
        hints.put(EncodeHintType.MARGIN, 0);
        return QR_CODE_WRITER.encode(contents, BarcodeFormat.QR_CODE, width, height, hints);
    }
}

