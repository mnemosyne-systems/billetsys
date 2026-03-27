/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.util;

import ai.mnemosyne_systems.model.Attachment;
import ai.mnemosyne_systems.model.Message;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

public final class AttachmentHelper {

    private static final Logger LOGGER = Logger.getLogger(AttachmentHelper.class);

    private AttachmentHelper() {
    }

    public static String readFormValue(MultipartFormDataInput input, String name) {
        if (input == null || name == null) {
            return null;
        }
        List<InputPart> parts = input.getFormDataMap().get(name);
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        try {
            return parts.get(0).getBodyAsString();
        } catch (IOException ex) {
            throw new BadRequestException("Invalid form data");
        }
    }

    public static Long readFormLong(MultipartFormDataInput input, String name) {
        String value = readFormValue(input, name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid form data");
        }
    }

    public static List<Attachment> readAttachments(MultipartFormDataInput input, String name) {
        if (input == null || name == null) {
            return List.of();
        }
        LOGGER.debugf("Attachment parts for field '%s': %s", name, listPartNames(input));
        List<InputPart> parts = collectParts(input, name);
        if (parts.isEmpty()) {
            LOGGER.debugf("No attachment parts found for field '%s'. Available parts: %s", name, listPartNames(input));
            return List.of();
        }
        LOGGER.debugf("Processing %d attachment part(s) for field '%s'", parts.size(), name);
        List<Attachment> attachments = new ArrayList<>();
        for (InputPart part : parts) {
            String fileName = extractFileName(part.getHeaders());
            if (fileName == null || fileName.isBlank()) {
                LOGGER.debug("Skipping attachment with blank filename");
                continue;
            }
            byte[] data = readBytes(part);
            if (data.length == 0) {
                LOGGER.debugf("Skipping attachment '%s' with empty payload", fileName);
                continue;
            }
            String mimeType = part.getMediaType() == null ? null : part.getMediaType().toString();
            mimeType = detectMimeType(mimeType, fileName, data);
            Attachment attachment = new Attachment();
            attachment.name = fileName;
            attachment.mimeType = mimeType;
            attachment.data = data;
            attachments.add(attachment);
            LOGGER.debugf("Prepared attachment '%s' (%s, %d bytes)", fileName, mimeType, data.length);
        }
        LOGGER.debugf("Prepared %d attachment(s) for field '%s'", attachments.size(), name);
        return attachments;
    }

    private static List<InputPart> collectParts(MultipartFormDataInput input, String name) {
        List<InputPart> parts = new ArrayList<>();
        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();
        if (formDataMap != null) {
            List<InputPart> named = formDataMap.get(name);
            if (named != null) {
                parts.addAll(named);
            }
            List<InputPart> namedArray = formDataMap.get(name + "[]");
            if (namedArray != null) {
                parts.addAll(namedArray);
            }
        }
        List<InputPart> allParts = input.getParts();
        if (allParts == null) {
            return parts;
        }
        for (InputPart part : allParts) {
            String partName = extractPartName(part.getHeaders());
            if (name.equals(partName) || (name + "[]").equals(partName)) {
                if (!parts.contains(part)) {
                    parts.add(part);
                }
            }
        }
        return parts;
    }

    public static void attachToMessage(Message message, List<Attachment> attachments) {
        if (message == null || attachments == null || attachments.isEmpty()) {
            return;
        }
        for (Attachment attachment : attachments) {
            attachment.message = message;
            message.attachments.add(attachment);
        }
        LOGGER.debugf("Attached %d attachment(s) to message", attachments.size());
    }

    public static void resolveInlineAttachmentUrls(Message message, List<Attachment> attachments) {
        if (message == null || message.body == null || message.body.isBlank() || attachments == null
                || attachments.isEmpty()) {
            return;
        }
        String updatedBody = message.body;
        for (Attachment attachment : attachments) {
            if (attachment == null || attachment.id == null || attachment.name == null || attachment.name.isBlank()) {
                continue;
            }
            String encodedName = URLEncoder.encode(attachment.name, StandardCharsets.UTF_8).replace("+", "%20");
            String url = "/attachments/" + attachment.id + "/data";
            updatedBody = updatedBody.replace("attachment://" + encodedName, url);
            updatedBody = updatedBody.replace("attachment://" + attachment.name, url);
        }
        message.body = updatedBody;
    }

    private static List<String> listPartNames(MultipartFormDataInput input) {
        List<String> names = new ArrayList<>();
        if (input == null) {
            return names;
        }
        List<InputPart> allParts = input.getParts();
        if (allParts == null) {
            return names;
        }
        for (InputPart part : allParts) {
            String partName = extractPartName(part.getHeaders());
            if (partName != null && !partName.isBlank()) {
                names.add(partName);
            }
        }
        return names;
    }

    private static byte[] readBytes(InputPart part) {
        try {
            byte[] data = part.getBody(byte[].class, null);
            return data == null ? new byte[0] : data;
        } catch (IOException ex) {
            throw new BadRequestException("Invalid attachment data");
        }
    }

    private static String extractFileName(MultivaluedMap<String, String> headers) {
        if (headers == null) {
            return null;
        }
        String contentDisposition = headers.getFirst("Content-Disposition");
        if (contentDisposition == null) {
            return null;
        }
        String[] segments = contentDisposition.split(";");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.startsWith("filename=")) {
                String fileName = trimmed.substring("filename=".length()).trim();
                if (fileName.startsWith("\"") && fileName.endsWith("\"") && fileName.length() > 1) {
                    fileName = fileName.substring(1, fileName.length() - 1);
                }
                return fileName;
            }
        }
        return null;
    }

    private static String extractPartName(MultivaluedMap<String, String> headers) {
        if (headers == null) {
            return null;
        }
        String contentDisposition = headers.getFirst("Content-Disposition");
        if (contentDisposition == null) {
            return null;
        }
        String[] segments = contentDisposition.split(";");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.startsWith("name=")) {
                String name = trimmed.substring("name=".length()).trim();
                if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 1) {
                    name = name.substring(1, name.length() - 1);
                }
                return name;
            }
        }
        return null;
    }

    private static String detectMimeType(String provided, String fileName, byte[] data) {
        if (provided != null && !provided.isBlank()) {
            String cleaned = provided.split(";", 2)[0].trim();
            if (!cleaned.isBlank() && !"application/octet-stream".equalsIgnoreCase(cleaned)) {
                return cleaned;
            }
        }
        String imageType = detectImageType(data);
        if (imageType != null) {
            return imageType;
        }
        String extension = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ENGLISH);
        if (extension.endsWith(".txt") || extension.endsWith(".log") || extension.endsWith(".md")) {
            return "text/plain";
        }
        if (extension.endsWith(".csv")) {
            return "text/csv";
        }
        if (extension.endsWith(".json")) {
            return "application/json";
        }
        if (extension.endsWith(".png")) {
            return "image/png";
        }
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (extension.endsWith(".gif")) {
            return "image/gif";
        }
        return "text/plain";
    }

    private static String detectImageType(byte[] data) {
        if (data == null || data.length < 4) {
            return null;
        }
        if (data.length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
            return "image/gif";
        }
        return null;
    }
}
