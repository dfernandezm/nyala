package com.nyala.core.infrastructure.pdf;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class PdfSplitter {

    public List<String> filesToExtract(String originalsFolder) {
        log.info("Listing files to extract...");
        try (Stream<Path> walk = Files.walk(Paths.get(originalsFolder))) {
            // We want to find only regular files
            List<String> result = walk
                    .filter(f -> Files.isRegularFile(f))
                    .filter(f -> f.getFileName().toString().endsWith(".pdf"))
                    .map(f -> f.toAbsolutePath().toString()).collect(Collectors.toList());
            log.info("Going to extract {} files:", result.size());
            log.info("File list: {}", String.join("\n", result));
            return result;
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
    }

    public void extractFirstPageOfPdf(String pdfFilePath) {
        log.info("Extracting first page of file: {}", pdfFilePath);
        try {
            Document document = new Document();
            PdfReader reader = new PdfReader(pdfFilePath);
            int page = 1;
            
            String destinationFilePath = pdfFilePath.replaceAll(".pdf", "_p1.pdf");
            PdfCopy copy = new PdfCopy(document, new FileOutputStream(destinationFilePath));

            document.open();
            
            copy.addPage(copy.getImportedPage(reader, page));
            copy.freeReader(reader);

            reader.close();
            document.close();

        } catch (IOException | DocumentException doe) {
            throw new RuntimeException("Error extracting pdf page", doe);
        }
    }

    public void extractAll(String originalsFolder) {
        log.info("Extracting first page of PDF files...");
        filesToExtract(originalsFolder).forEach(this::extractFirstPageOfPdf);
    }

    public static void main(String[] args) {
        PdfSplitter pdfSplitter = new PdfSplitter();
        String originalsFolder = "/Users/david/Documents/Citizenship/utility-bills/bills";
        pdfSplitter.extractAll(originalsFolder);
    }
}
