package com.cafeteria;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.cafeteria.descuento.CalculadoraDescuento;
import com.cafeteria.descuento.ReglaDescuento;
import com.cafeteria.descuento.ReglasDescuento;
import com.cafeteria.descuento.ResultadoDescuento;
import com.cafeteria.modelo.Bebida;
import com.cafeteria.modelo.Comida;
import com.cafeteria.modelo.Producto;
import com.cafeteria.persistencia.ConexionBD;
import com.cafeteria.persistencia.PedidoRepositorio;

/**
 * Pruebas sencillas, sin frameworks externos, de la lógica de la aplicación.
 * Ejecutar: java -cp "out;lib\sqlite-jdbc-3.46.1.3.jar" com.cafeteria.PruebasCafeteria
 */
public final class PruebasCafeteria {

    private static int aprobadas;
    private static int fallidas;

    private PruebasCafeteria() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Pruebas de la calculadora de descuentos ===");
        CalculadoraDescuento calculadora = new CalculadoraDescuento(List.of(ReglasDescuento.POR_MONTO));
        verificar("$8.00 no tiene descuento", calculadora.calcular(8.00).totalFinal(), 8.00);
        verificar("$10.00 no tiene descuento (debe ser mayor a 10)", calculadora.calcular(10.00).totalFinal(), 10.00);
        verificar("$10.01 tiene 15 % de descuento", calculadora.calcular(10.01).descuento(), 1.50);
        verificar("$20.00 paga $17.00", calculadora.calcular(20.00).totalFinal(), 17.00);
        verificar("$0.00 paga $0.00", calculadora.calcular(0).totalFinal(), 0.00);

        System.out.println("\n=== Principio abierto/cerrado: nueva regla sin modificar la calculadora ===");
        ReglaDescuento promocion = ReglasDescuento.porMontoMinimo(50, 0.20);
        CalculadoraDescuento conPromocion = new CalculadoraDescuento(List.of(ReglasDescuento.POR_MONTO, promocion));
        verificar("$60.00 con promoción del 20 % paga $48.00", conPromocion.calcular(60).totalFinal(), 48.00);
        verificar("$30.00 sigue con el 15 % y paga $25.50", conPromocion.calcular(30).totalFinal(), 25.50);

        System.out.println("\n=== Polimorfismo: cada producto calcula su precio ===");
        Producto bebida = new Bebida("Capuchino", 2.50, Bebida.Tamano.GRANDE);
        Producto comida = new Comida("Sándwich", 4.50, true);
        verificar("Capuchino grande = 2.50 + 1.00", bebida.calcularPrecio(), 3.50);
        verificar("Sándwich para llevar = 4.50 + 0.30", comida.calcularPrecio(), 4.80);

        System.out.println("\n=== Persistencia con JDBC ===");
        Path archivo = Files.createTempFile("cafeteria_prueba", ".db");
        PedidoRepositorio repositorio = new PedidoRepositorio(new ConexionBD("jdbc:sqlite:" + archivo));
        System.out.println("  " + repositorio.inicializar());
        ResultadoDescuento resultado = calculadora.calcular(12.50);
        verificar("El primer pedido guardado recibe el id 1", repositorio.guardar(resultado), 1);
        verificar("El segundo pedido guardado recibe el id 2", repositorio.guardar(resultado), 2);
        Files.deleteIfExists(archivo);

        System.out.printf("%nResultado: %d pruebas aprobadas, %d fallidas%n", aprobadas, fallidas);
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static void verificar(String descripcion, double obtenido, double esperado) {
        boolean ok = Math.abs(obtenido - esperado) < 0.005;
        if (ok) {
            aprobadas++;
        } else {
            fallidas++;
        }
        System.out.printf("  [%s] %s (obtenido: %.2f)%n", ok ? "OK" : "FALLA", descripcion, obtenido);
    }
}
