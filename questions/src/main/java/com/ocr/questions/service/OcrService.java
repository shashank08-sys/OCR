package com.ocr.questions.service;

import com.ocr.questions.entity.OcrResult;
import com.ocr.questions.repository.OcrResultRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class OcrService {

    @Autowired
    private OcrResultRepository ocrResultRepository;

    /**
     * Extract text from a PDF file using Apache PDFBox
     */
    public OcrResult extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            OcrResult result = new OcrResult();
            result.setFileName(file.getOriginalFilename());
            result.setFileType("PDF");
            result.setExtractedText(extractedText.trim());

            return ocrResultRepository.save(result);
        }
    }

    /**
     * Extract raw text from PDF without saving to DB (used by question parser)
     */
    public String extractRawTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }

    public List<OcrResult> getAllResults() {
        return ocrResultRepository.findAll();
    }

    public OcrResult getResultById(Long id) {
        return ocrResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OCR Result not found with id: " + id));
    }

    public List<OcrResult> getResultsByType(String fileType) {
        return ocrResultRepository.findByFileType(fileType.toUpperCase());
    }

    public void deleteResult(Long id) {
        ocrResultRepository.deleteById(id);
    }
}
