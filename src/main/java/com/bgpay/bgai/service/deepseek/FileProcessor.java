package com.bgpay.bgai.service.deepseek;

import com.bgpay.bgai.entity.MimeTypeConfig;
import com.bgpay.bgai.service.impl.FileTypeService;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class FileProcessor {
    private final FileTypeService fileTypeService;
    private static final ThreadLocal<Tesseract> TESSERACT_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/path/to/your/tessdata");
        tesseract.setLanguage("chi_sim+eng");
        tesseract.setPageSegMode(1);  // 自动页面分割
        return tesseract;
    });

    public FileProcessor(FileTypeService fileTypeService) {
        this.fileTypeService = fileTypeService;
    }

    public String processFile(MultipartFile file) throws Exception {
        String contentType = validateFile(file);
        File tempFile = createTempFile(file);

        try {
            if (!fileTypeService.validateFileMagic(tempFile, contentType)) {
                throw new IllegalArgumentException("文件内容与类型不匹配");
            }

            return switch (contentType.toLowerCase()) {
                case "image/png", "image/jpeg", "image/tiff", "image/bmp", "image/gif" -> processImage(tempFile);
                case "application/pdf" -> processPDF(tempFile);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> processDocx(tempFile);
                case "application/vnd.ms-excel" -> processExcel(tempFile, false);
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> processExcel(tempFile, true);
                case "application/vnd.ms-powerpoint" -> processPresentation(tempFile, false);
                case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> processPresentation(tempFile, true);
                case "video/mp4", "video/quicktime" -> processVideo(tempFile);
                // 新增文本类文件处理
                case "text/x-python", "application/javascript", "application/typescript", "text/x-ruby", "text/x-perl",
                     "text/x-sh", "application/powershell", "text/html", "application/xml", "application/xslt+xml",
                     "text/markdown", "text/x-java-source", "text/x-c", "text/x-c++", "text/x-csharp",
                     "application/x-php", "text/x-go", "text/x-rust", "text/x-swift", "text/plain",
                     "application/x-win-registry", "application/json", "text/yaml", "text/x-properties",
                     "text/css", "application/sql", "text/x-makefile", "text/x-asm", "application/coffeescript",
                     "application/dart", "text/x-erlang", "text/x-fortran", "text/x-groovy", "text/x-haskell",
                     "text/x-lua", "text/x-objective-c", "text/x-pascal", "text/x-scala", "text/x-vhdl",
                     "text/x-verilog" -> processTextFile(tempFile);
                default -> throw new IllegalArgumentException("不支持的文件类型: " + contentType);
            };
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }

    // 新增文件编码检测方法
    private static String detectCharset(File file) throws IOException {
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[4096];
            input.read(buffer);
            String charset = "UTF-8";

            // 简单编码探测
            if (buffer.length >= 3 &&
                    (buffer[0] & 0xFF) == 0xEF &&
                    (buffer[1] & 0xFF) == 0xBB &&
                    (buffer[2] & 0xFF) == 0xBF) {
                charset = "UTF-8";
            } else if (buffer.length >= 2 &&
                    (buffer[0] & 0xFF) == 0xFE &&
                    (buffer[1] & 0xFF) == 0xFF) {
                charset = "UTF-16BE";
            } else if (buffer.length >= 2 &&
                    (buffer[0] & 0xFF) == 0xFF &&
                    (buffer[1] & 0xFF) == 0xFE) {
                charset = "UTF-16LE";
            }
            return charset;
        }
    }

    // 优化后的文本文件处理方法（支持编码检测）
    private String processTextFile(File file) throws IOException {
        String charset = detectCharset(file);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            StringBuilder content = new StringBuilder((int) file.length());
            char[] buffer = new char[8192];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, charsRead);
            }
            return String.format("【文本内容】\n%s", content.toString());
        }
    }

    private String validateFile(MultipartFile file) {
        String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                .orElseThrow(() -> new IllegalArgumentException("文件名不能为空"));

        String contentType = Optional.ofNullable(file.getContentType())
                .orElseThrow(() -> new IllegalArgumentException("无法识别文件类型"));

        String extension = getFileExtension(originalFilename);
        // 增强扩展名检查
        if (!extension.isEmpty()) {
            MimeTypeConfig config = fileTypeService.getExtensionToMimeTypeConfig().get(extension.toLowerCase());
            if (config != null) {
                // 特殊处理无MIME类型的文本文件
                if (config.getMimeType().startsWith("text/")) {
                    if (!contentType.startsWith("text/")) {
                        throw new IllegalArgumentException(String.format(
                                "文本文件类型不匹配（扩展名: %s, 预期类型: %s, 实际类型: %s）",
                                extension, config.getMimeType(), contentType
                        ));
                    }
                } else if (!config.getMimeType().equalsIgnoreCase(contentType)) {
                    throw new IllegalArgumentException(String.format(
                            "文件类型不匹配（扩展名: %s, 预期类型: %s, 实际类型: %s）",
                            extension, config.getMimeType(), contentType
                    ));
                }
            }
        }
        if (!extension.isEmpty()) {
            MimeTypeConfig config = fileTypeService.getExtensionToMimeTypeConfig().get(extension.toLowerCase());
            if (config != null && !config.getMimeType().equalsIgnoreCase(contentType)) {
                throw new IllegalArgumentException(String.format(
                        "文件类型不匹配（扩展名: %s, 预期类型: %s, 实际类型: %s）",
                        extension, config.getMimeType(), contentType
                ));
            }
        }

        if (!fileTypeService.isAllowedType(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }

        return contentType;
    }

    private String processDocx(File file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String processExcel(File file, boolean isXlsx) throws Exception {
        try (Workbook workbook = isXlsx ?
                new XSSFWorkbook(file) :
                new HSSFWorkbook(new FileInputStream(file))) {
            return extractExcelContent(workbook);
        }
    }

    private String extractExcelContent(Workbook workbook) {
        StringBuilder content = new StringBuilder(1024 * 1024);  // 预分配1MB容量
        for (Sheet sheet : workbook) {
            content.append("工作表：").append(sheet.getSheetName()).append('\n');
            for (Row row : sheet) {
                for (Cell cell : row) {
                    appendCellValue(content, cell);
                    content.append('\t');
                }
                content.append('\n');
            }
        }
        return content.toString();
    }

    private void appendCellValue(StringBuilder sb, Cell cell) {
        switch (cell.getCellType()) {
            case STRING -> sb.append(cell.getStringCellValue());
            case NUMERIC -> sb.append(String.format("%.2f", cell.getNumericCellValue()));
            case BOOLEAN -> sb.append(cell.getBooleanCellValue());
            case FORMULA -> sb.append(cell.getCellFormula());
            default -> {}
        }
    }

    private String processPresentation(File file, boolean isPptx) throws Exception {
        try (SlideShow<?, ?> slideShow = isPptx ?
                new XMLSlideShow(new FileInputStream(file)) :
                new HSLFSlideShow(new FileInputStream(file))) {
            return extractPresentationContent(slideShow);
        }
    }

    private String extractPresentationContent(SlideShow<?, ?> slideShow) {
        StringBuilder content = new StringBuilder(1024 * 1024);
        for (Slide slide : slideShow.getSlides()) {
            slide.getShapes().stream()
                    .filter(shape -> shape instanceof TextShape)
                    .map(shape -> (TextShape<?, ?>) shape)
                    .forEach(textShape -> {
                        // 尝试进行类型转换
                        if (textShape instanceof org.apache.poi.sl.usermodel.TextShape<?, ?>) {
                            org.apache.poi.sl.usermodel.TextShape<?, ?> typedTextShape = (org.apache.poi.sl.usermodel.TextShape<?, ?>) textShape;
                            content.append(typedTextShape.getText()).append('\n');
                        }
                    });
        }
        return content.toString();
    }

    private static String processImage(File file) throws Exception {
        String metadata = extractImageMetadata(file);
        String ocrText = performOCR(file);
        return formatResult(metadata, ocrText);
    }

    private static String extractImageMetadata(File file) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return "";

            ImageReader reader = readers.next();
            reader.setInput(iis);
            return String.format("""
                【图像元数据】
                格式：%s
                尺寸：%dx%d
                色彩模式：%s""",
                    reader.getFormatName(),
                    reader.getWidth(0),
                    reader.getHeight(0),
                    reader.getRawImageType(0));
        } catch (IOException e) {
            return "元数据提取失败";
        }
    }

    private static String performOCR(File file) throws TesseractException, IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) throw new IOException("无法读取图像文件");
        BufferedImage rgbImage = convertToRGB(image);
        return TESSERACT_THREAD_LOCAL.get().doOCR(rgbImage);
    }

    private static BufferedImage convertToRGB(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) return image;
        BufferedImage rgbImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        rgbImage.createGraphics().drawImage(image, 0, 0, null);
        return rgbImage;
    }

    private static File createTempFile(MultipartFile file) throws IOException {
        String suffix = Optional.ofNullable(file.getOriginalFilename())
                .map(fn -> "." + getFileExtension(fn))
                .orElseGet(() -> ".tmp");

        File tempFile = Files.createTempFile("upload-", suffix + ThreadLocalRandom.current().nextInt()).toFile();
        try (InputStream is = file.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            is.transferTo(fos);
        }
        return tempFile;
    }

    private static String formatResult(String metadata, String content) {
        return String.format("%s\n\n【识别内容】\n%s", metadata, content);
    }

    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private static String processPDF(File file) throws IOException {
        try (PDDocument doc = PDDocument.load(file)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static String processVideo(File file) throws Exception {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();
            return String.format("""
                视频元数据：
                格式：%s
                时长：%.1f秒
                分辨率：%dx%d
                帧率：%.1f fps""",
                    grabber.getFormat(),
                    grabber.getLengthInTime() / 1_000_000.0,
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getVideoFrameRate()
            );
        }
    }

    public String processFile(File file, String contentType) throws Exception {
        // 验证文件类型（新增方法）
        String validContentType = validateFileType(file, file.getName(), contentType);

        return switch (contentType.toLowerCase()) {
            case "image/png", "image/jpeg", "image/tiff", "image/bmp", "image/gif" -> processImage(file);
            case "application/pdf" -> processPDF(file);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> processDocx(file);
            case "application/vnd.ms-excel" -> processExcel(file, false);
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> processExcel(file, true);
            case "application/vnd.ms-powerpoint" -> processPresentation(file, false);
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> processPresentation(file, true);
            case "video/mp4", "video/quicktime" -> processVideo(file);
            // 新增文本类文件处理
            case "text/x-python", "application/javascript", "application/typescript", "text/x-ruby", "text/x-perl",
                 "text/x-sh", "application/powershell", "text/html", "application/xml", "application/xslt+xml",
                 "text/markdown", "text/x-java-source", "text/x-c", "text/x-c++", "text/x-csharp",
                 "application/x-php", "text/x-go", "text/x-rust", "text/x-swift", "text/plain",
                 "application/x-win-registry", "application/json", "text/yaml", "text/x-properties",
                 "text/css", "application/sql", "text/x-makefile", "text/x-asm", "application/coffeescript",
                 "application/dart", "text/x-erlang", "text/x-fortran", "text/x-groovy", "text/x-haskell",
                 "text/x-lua", "text/x-objective-c", "text/x-pascal", "text/x-scala", "text/x-vhdl",
                 "text/x-verilog" -> processTextFile(file);
            default -> throw new IllegalArgumentException("不支持的文件类型: " + validContentType);
        };
    }

    private String validateFileType(File file, String filename, String contentType) {
        String extension = getFileExtension(filename);
        MimeTypeConfig config = fileTypeService.getExtensionToMimeTypeConfig().get(extension);

        if (config != null && !config.getMimeType().equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("文件类型不匹配");
        }
        return contentType;
    }
}