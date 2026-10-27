package com.clinica.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cita_id", nullable = false, unique = true)
    private Integer citaId;

    @Column(nullable = false)
    private Double monto;

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    @Column(nullable = false)
    private String estado;

    @Column(name = "numero_voucher", unique = true)
    private String numeroVoucher;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCitaId() { return citaId; }
    public void setCitaId(Integer citaId) { this.citaId = citaId; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getNumeroVoucher() { return numeroVoucher; }
    public void setNumeroVoucher(String numeroVoucher) { this.numeroVoucher = numeroVoucher; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}