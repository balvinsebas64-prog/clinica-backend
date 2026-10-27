package com.clinica.controladores;

import com.clinica.entidades.Cita;
import com.clinica.entidades.Seguro;
import com.clinica.repositorios.CitaRepository;
import com.clinica.repositorios.SeguroRepository;
import com.clinica.repositorios.MedicoRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ReporteController {

    @Autowired
    private SeguroRepository seguroRepo;

    @Autowired
    private CitaRepository citaRepo;

    @Autowired
    private MedicoRepository medicoRepo;

    // ==========================================
    // 1. EXPORTAR SEGUROS A PDF
    // ==========================================
    @GetMapping("/admin/reportes/seguros-pdf")
    public void exportarSegurosPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Reporte_Seguros_" + fechaActual + ".pdf";
        response.setHeader(headerKey, headerValue);

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        com.lowagie.text.Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        com.lowagie.text.Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
        com.lowagie.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        com.lowagie.text.Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph titulo = new Paragraph("Hospital EsSalud", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph("Reporte de Estado de Seguros\nGenerado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontSubtitulo);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(20);
        document.add(subtitulo);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 3f, 2f, 3f, 2f});

        String[] headers = {"ID", "Paciente", "DNI", "N° Afiliación", "Estado"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(new Color(0, 102, 204));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        List<Seguro> seguros = seguroRepo.findAll();
        for (Seguro s : seguros) {
            PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(s.getId()), fontBody));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c1);

            table.addCell(new PdfPCell(new Phrase(s.getPaciente().getNombre() + " " + s.getPaciente().getApellido(), fontBody)));
            table.addCell(new PdfPCell(new Phrase(s.getPaciente().getDni(), fontBody)));
            table.addCell(new PdfPCell(new Phrase(s.getNumeroAfiliacion(), fontBody)));
            
            PdfPCell cEstado = new PdfPCell(new Phrase(s.getEstado().name(), fontBody));
            cEstado.setHorizontalAlignment(Element.ALIGN_CENTER);
            if (s.getEstado().name().equals("ACTIVO")) {
                cEstado.setBackgroundColor(new Color(200, 255, 200));
            } else if (s.getEstado().name().equals("MOROSO")) {
                cEstado.setBackgroundColor(new Color(255, 200, 200));
            }
            table.addCell(cEstado);
        }

        document.add(table);
        document.close();
    }

    // ==========================================
    // 2. EXPORTAR SEGUROS A EXCEL
    // ==========================================
    @GetMapping("/admin/reportes/seguros-excel")
    public void exportarSegurosExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Reporte_Seguros_" + fechaActual + ".xlsx";
        response.setHeader(headerKey, headerValue);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Seguros");

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Paciente", "DNI", "N° Afiliación", "Estado"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Seguro> seguros = seguroRepo.findAll();
        int rowNum = 1;
        for (Seguro s : seguros) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(s.getId());
            row.createCell(1).setCellValue(s.getPaciente().getNombre() + " " + s.getPaciente().getApellido());
            row.createCell(2).setCellValue(s.getPaciente().getDni());
            row.createCell(3).setCellValue(s.getNumeroAfiliacion());
            row.createCell(4).setCellValue(s.getEstado().name());
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ==========================================
    // 3. EXPORTAR CITAS DEL DÍA A PDF
    // ==========================================
    @GetMapping("/admin/reportes/citas-pdf")
    public void exportarCitasPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Reporte_Citas_" + fechaActual + ".pdf";
        response.setHeader(headerKey, headerValue);

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        com.lowagie.text.Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        com.lowagie.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        com.lowagie.text.Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph titulo = new Paragraph("Hospital EsSalud - Citas del Día", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph("Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontBody);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(20);
        document.add(subtitulo);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 3f, 3f, 2f, 2f});

        String[] headers = {"ID", "Paciente", "Médico", "Hora", "Estado"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(new Color(0, 102, 204));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        List<Cita> citasHoy = citaRepo.findAll().stream()
            .filter(c -> c.getFechaHora().toLocalDate().equals(LocalDate.now()))
            .collect(Collectors.toList());

        for (Cita c : citasHoy) {
            table.addCell(new PdfPCell(new Phrase(String.valueOf(c.getId()), fontBody)));
            table.addCell(new PdfPCell(new Phrase(c.getPaciente().getNombre() + " " + c.getPaciente().getApellido(), fontBody)));
            table.addCell(new PdfPCell(new Phrase(c.getMedico() != null ? c.getMedico().getNombre() : "Sin asignar", fontBody)));
            table.addCell(new PdfPCell(new Phrase(c.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm")), fontBody)));
            table.addCell(new PdfPCell(new Phrase(c.getEstado().name(), fontBody)));
        }

        document.add(table);
        document.close();
    }

    // ==========================================
    // 4. EXPORTAR CITAS DEL DÍA A EXCEL
    // ==========================================
    @GetMapping("/admin/reportes/citas-excel")
    public void exportarCitasExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Reporte_Citas_" + fechaActual + ".xlsx";
        response.setHeader(headerKey, headerValue);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Citas del Día");

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Paciente", "Médico", "Hora", "Estado"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Cita> citasHoy = citaRepo.findAll().stream()
            .filter(c -> c.getFechaHora().toLocalDate().equals(LocalDate.now()))
            .collect(Collectors.toList());

        int rowNum = 1;
        for (Cita c : citasHoy) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getPaciente().getNombre() + " " + c.getPaciente().getApellido());
            row.createCell(2).setCellValue(c.getMedico() != null ? c.getMedico().getNombre() : "Sin asignar");
            row.createCell(3).setCellValue(c.getFechaHora().format(DateTimeFormatter.ofPattern("HH:mm")));
            row.createCell(4).setCellValue(c.getEstado().name());
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ==========================================
    // 5. EXPORTAR TODOS LOS MÉDICOS A PDF
    // ==========================================
    @GetMapping("/admin/reportes/medicos-pdf")
    public void exportarMedicosPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        response.setHeader("Content-Disposition", "attachment; filename=Reporte_Medicos_" + fechaActual + ".pdf");

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        com.lowagie.text.Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        com.lowagie.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        com.lowagie.text.Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph titulo = new Paragraph("Hospital EsSalud - Lista de Médicos", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        
        Paragraph subtitulo = new Paragraph("Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontBody);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(20);
        document.add(subtitulo);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 3f, 2f, 2f, 2f, 2f});

        String[] headers = {"ID", "Nombre Completo", "DNI", "Teléfono", "Especialidad", "CMP"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(new Color(0, 102, 204));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        List<com.clinica.entidades.Medico> medicos = medicoRepo.findAll();
        for (com.clinica.entidades.Medico m : medicos) {
            table.addCell(new PdfPCell(new Phrase(String.valueOf(m.getId()), fontBody)));
            table.addCell(new PdfPCell(new Phrase(m.getNombre() + " " + m.getApellido(), fontBody)));
            table.addCell(new PdfPCell(new Phrase(m.getDni() != null ? m.getDni() : "-", fontBody)));
            table.addCell(new PdfPCell(new Phrase(m.getTelefono() != null ? m.getTelefono() : "-", fontBody)));
            
            String especialidad = "-";
            if (m.getEspecialidad() != null) {
                especialidad = m.getEspecialidad().getNombre(); 
            }
            table.addCell(new PdfPCell(new Phrase(especialidad, fontBody)));
            
            // ⭐ CORREGIDO AQUÍ (getNumeroColegiatura)
            table.addCell(new PdfPCell(new Phrase(m.getNumeroColegiatura() != null ? m.getNumeroColegiatura() : "-", fontBody)));
        } // ⭐ FALTABA ESTA LLAVE DE CIERRE DEL FOR

        document.add(table);
        document.close();
    }

    // ==========================================
    // 6. EXPORTAR TODOS LOS MÉDICOS A EXCEL
    // ==========================================
    @GetMapping("/admin/reportes/medicos-excel")
    public void exportarMedicosExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        response.setHeader("Content-Disposition", "attachment; filename=Reporte_Medicos_" + fechaActual + ".xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Médicos");

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Nombre Completo", "DNI", "Teléfono", "Especialidad", "CMP"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        List<com.clinica.entidades.Medico> medicos = medicoRepo.findAll();
        int rowNum = 1;
        for (com.clinica.entidades.Medico m : medicos) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(m.getId());
            row.createCell(1).setCellValue(m.getNombre() + " " + m.getApellido());
            row.createCell(2).setCellValue(m.getDni() != null ? m.getDni() : "-");
            row.createCell(3).setCellValue(m.getTelefono() != null ? m.getTelefono() : "-");
            
            String especialidad = "-";
            if (m.getEspecialidad() != null) {
                especialidad = m.getEspecialidad().getNombre(); 
            }
            row.createCell(4).setCellValue(especialidad);
            
            // ⭐ CORREGIDO AQUÍ (getNumeroColegiatura)
            row.createCell(5).setCellValue(m.getNumeroColegiatura() != null ? m.getNumeroColegiatura() : "-");
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    // ==========================================
    // 7. EXPORTAR TODAS LAS CITAS A PDF
    // ==========================================
    @GetMapping("/admin/reportes/todas-citas-pdf")
    public void exportarTodasCitasPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        response.setHeader("Content-Disposition", "attachment; filename=Reporte_Todas_Citas_" + fechaActual + ".pdf");

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        com.lowagie.text.Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        com.lowagie.text.Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        com.lowagie.text.Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph titulo = new Paragraph("Hospital EsSalud - Historial de Citas", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        
        Paragraph subtitulo = new Paragraph("Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontBody);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(20);
        document.add(subtitulo);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 3f, 3f, 2f, 2f});

        String[] headers = {"ID", "Paciente", "Médico", "Fecha y Hora", "Estado"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(new Color(0, 102, 204));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        List<Cita> citas = citaRepo.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Cita c : citas) {
            table.addCell(new PdfPCell(new Phrase(String.valueOf(c.getId()), fontBody)));
            table.addCell(new PdfPCell(new Phrase(c.getPaciente().getNombre() + " " + c.getPaciente().getApellido(), fontBody)));
            table.addCell(new PdfPCell(new Phrase(c.getMedico() != null ? c.getMedico().getNombre() + " " + c.getMedico().getApellido() : "Sin asignar", fontBody)));
            table.addCell(new PdfPCell(new Phrase(c.getFechaHora().format(formatter), fontBody)));
            table.addCell(new PdfPCell(new Phrase(c.getEstado().name(), fontBody)));
        }

        document.add(table);
        document.close();
    }

    // ==========================================
    // 8. EXPORTAR TODAS LAS CITAS A EXCEL
    // ==========================================
    @GetMapping("/admin/reportes/todas-citas-excel")
    public void exportarTodasCitasExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fechaActual = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        response.setHeader("Content-Disposition", "attachment; filename=Reporte_Todas_Citas_" + fechaActual + ".xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Todas las Citas");

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Paciente", "Médico", "Fecha y Hora", "Estado"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Cita> citas = citaRepo.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        int rowNum = 1;
        
        for (Cita c : citas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getPaciente().getNombre() + " " + c.getPaciente().getApellido());
            row.createCell(2).setCellValue(c.getMedico() != null ? c.getMedico().getNombre() + " " + c.getMedico().getApellido() : "Sin asignar");
            row.createCell(3).setCellValue(c.getFechaHora().format(formatter));
            row.createCell(4).setCellValue(c.getEstado().name());
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}