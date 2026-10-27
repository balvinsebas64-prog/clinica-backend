package com.clinica.entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_seguro")
public class PagoSeguro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "seguro_id", nullable = false)
    private Seguro seguro;

    @Column(name = "mes_pagado", nullable = false, length = 7)
    private String mesPagado; // Ejemplo: "2026-10"

    @Column(nullable = false)
    private Double monto;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado = EstadoPago.PAGADO;

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Seguro getSeguro() { return seguro; }
    public void setSeguro(Seguro seguro) { this.seguro = seguro; }
    public String getMesPagado() { return mesPagado; }
    public void setMesPagado(String mesPagado) { this.mesPagado = mesPagado; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }
}