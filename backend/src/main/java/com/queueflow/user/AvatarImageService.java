package com.queueflow.user;

import com.queueflow.common.BusinessException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarImageService {
    static final int OUTPUT_SIZE = 640;
    static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    static final long MAX_PIXELS = 16_000_000L;

    ProcessedAvatar process(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_BYTES) throw invalidAvatar();
        try {
            byte[] input = file.getBytes();
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(input));
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0
                    || (long) source.getWidth() * source.getHeight() > MAX_PIXELS) {
                throw invalidAvatar();
            }

            int cropSize = Math.min(source.getWidth(), source.getHeight());
            int sourceX = (source.getWidth() - cropSize) / 2;
            int sourceY = (source.getHeight() - cropSize) / 2;
            BufferedImage normalized = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = normalized.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(source, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE,
                        sourceX, sourceY, sourceX + cropSize, sourceY + cropSize, null);
            } finally {
                graphics.dispose();
            }

            var output = new ByteArrayOutputStream();
            if (!ImageIO.write(normalized, "jpeg", output)) throw invalidAvatar();
            return new ProcessedAvatar(output.toByteArray(), "image/jpeg");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidAvatar();
        }
    }

    private BusinessException invalidAvatar() {
        return new BusinessException("INVALID_AVATAR",
                "Envie uma imagem JPG, PNG ou WebP válida de até 2 MB.", HttpStatus.BAD_REQUEST);
    }

    record ProcessedAvatar(byte[] content, String contentType) {}
}
