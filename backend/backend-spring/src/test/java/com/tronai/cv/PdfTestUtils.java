package com.tronai.cv;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PdfTestUtils {

    public static byte[] createPdf(String text, int pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (text != null && !text.isEmpty()) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(50, 700);
                        String[] lines = text.split("\\r?\\n");
                        for (int j = 0; j < lines.length; j++) {
                            String line = lines[j].trim();
                            if (!line.isEmpty()) {
                                stream.showText(line);
                            }
                            if (j < lines.length - 1) {
                                stream.newLineAtOffset(0, -15);
                            }
                        }
                        stream.endText();
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    public static byte[] createEncryptedPdf(String text, String password) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (text != null && !text.isEmpty()) {
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    String[] lines = text.split("\\r?\\n");
                    for (int j = 0; j < lines.length; j++) {
                        String line = lines[j].trim();
                        if (!line.isEmpty()) {
                            stream.showText(line);
                        }
                        if (j < lines.length - 1) {
                            stream.newLineAtOffset(0, -15);
                        }
                    }
                    stream.endText();
                }
            }
            AccessPermission permission = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, permission);
            policy.setEncryptionKeyLength(128);
            document.protect(policy);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
