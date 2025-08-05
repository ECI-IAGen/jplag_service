package co.edu.eci.arep.microspring.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GreetingControllerTest {

    @BeforeEach
    public void setUp() {
        // Si es necesario, se podría reinicializar el cache del GreetingController.
        // Por ejemplo, si se implementara un método para limpiar el cache.
        // GreetingController.clearCache();
    }

    @Test
    public void testGreetingDefault() {
        // Llamada con el valor por defecto "World"
        Greeting greeting = GreetingController.greeting("World");
        assertNotNull(greeting, "El saludo no debe ser nulo");
        assertTrue(greeting.getContent().contains("World"),
                "El contenido debe incluir 'World'");
    }

    @Test
    public void testGreetingCustomName() {
        // Llamada con un nombre personalizado
        Greeting greeting = GreetingController.greeting("Juan");
        assertNotNull(greeting, "El saludo no debe ser nulo");
        assertTrue(greeting.getContent().contains("Juan"),
                "El contenido debe incluir 'Juan'");
    }

    @Test
    public void testGreetingCache() {
        // Se espera que dos consultas con el mismo nombre retornen el mismo objeto
        // (mismo id y contenido)
        Greeting greeting1 = GreetingController.greeting("Maria");
        Greeting greeting2 = GreetingController.greeting("Maria");
        assertNotNull(greeting1, "El primer saludo no debe ser nulo");
        assertNotNull(greeting2, "El segundo saludo no debe ser nulo");
        assertEquals(greeting1.getId(), greeting2.getId(),
                "Los IDs deben ser iguales para el mismo nombre");
        assertEquals(greeting1.getContent(), greeting2.getContent(),
                "Los contenidos deben ser iguales para el mismo nombre");
    }

    @Test
    public void testGreetingEmptyName() {
        // Se prueba el comportamiento cuando se envía una cadena vacía
        Greeting greeting = GreetingController.greeting("");
        assertNotNull(greeting, "El saludo no debe ser nulo");
        // Se espera que se use la cadena vacía en el mensaje
        assertTrue(greeting.getContent().contains("Hello, !"),
                "El contenido debe reflejar el nombre vacío");
    }

    @Test
    public void testGreetingNull() {
        // Probamos enviar null directamente.
        // Dependiendo de la implementación, String.format manejará null convirtiéndolo
        // en "null".
        Greeting greeting = GreetingController.greeting(null);
        assertNotNull(greeting, "El saludo no debe ser nulo");
        assertTrue(greeting.getContent().contains("null"),
                "El contenido debe incluir 'null' cuando se pasa null");
    }

    @Test
    public void testGreetingDifferentNames() {
        // Se espera que nombres diferentes generen saludos distintos (diferentes IDs)
        Greeting greeting1 = GreetingController.greeting("Alice");
        Greeting greeting2 = GreetingController.greeting("Bob");
        assertNotNull(greeting1, "El saludo para Alice no debe ser nulo");
        assertNotNull(greeting2, "El saludo para Bob no debe ser nulo");
        assertNotEquals(greeting1.getId(), greeting2.getId(),
                "Los IDs deben ser diferentes para nombres distintos");
    }

    @Test
    public void testGreetingCaseSensitivity() {
        // Se espera que "Maria" y "maria" se traten como nombres diferentes (a menos
        // que se modifique para ignorar mayúsculas)
        Greeting greeting1 = GreetingController.greeting("Maria");
        Greeting greeting2 = GreetingController.greeting("maria");
        assertNotNull(greeting1, "El saludo para 'Maria' no debe ser nulo");
        assertNotNull(greeting2, "El saludo para 'maria' no debe ser nulo");
        assertNotEquals(greeting1.getId(), greeting2.getId(),
                "Los IDs deben ser diferentes para 'Maria' y 'maria'");
    }

    @Test
    public void testGreetingWhitespace() {
        // Se prueba enviar un nombre con espacios
        Greeting greeting = GreetingController.greeting("   ");
        assertNotNull(greeting, "El saludo no debe ser nulo");
        // Se espera que se conserven los espacios en el mensaje
        assertTrue(greeting.getContent().contains("   "),
                "El contenido debe conservar los espacios enviados");
    }
}
