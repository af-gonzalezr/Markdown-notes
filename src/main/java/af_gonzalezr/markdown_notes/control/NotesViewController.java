package af_gonzalezr.markdown_notes.control;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class NotesViewController {

    private final String CARPETA_NOTAS = "src/main/resources/carpeta_notas";

    @GetMapping("/")
    public String index(Model model) {
        try {
            Path path = Paths.get(CARPETA_NOTAS);
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }
            try (Stream<Path> stream = Files.list(path)) {
                List<String> notas = stream
                        .filter(file -> !Files.isDirectory(file))
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .collect(Collectors.toList());
                model.addAttribute("notas", notas);
            }
        } catch (IOException e) {
            model.addAttribute("error", "No se pudo cargar la lista de notas.");
        }
        return "index";
    }

    @GetMapping("/form")
    public String getform(@RequestParam(required = false) String archivo, Model model) {
        if (archivo != null) {
            try {
                Path path = Paths.get(CARPETA_NOTAS, archivo);
                if (Files.exists(path)) {
                    String contenido = Files.readString(path);
                    model.addAttribute("tituloOriginal", archivo);
                    model.addAttribute("textoOriginal", contenido);
                    model.addAttribute("editando", true);
                }
            } catch (IOException e) {
                model.addAttribute("errorG", "Error al leer la nota para editar.");
            }
        }
        return "form";
    }

    @PostMapping("/nota/guardar")
    public String guardar(@RequestParam("titulo") String titulo,
                         @RequestParam("Md") String Md,
                         @RequestParam(value = "tituloAnterior", required = false) String tituloAnterior,
                         Model model) {

        if (titulo == null || titulo.trim().isEmpty() || Md == null || Md.trim().isEmpty()) {
            model.addAttribute("errorG", "El título y el contenido no pueden estar vacíos.");
            model.addAttribute("textoOriginal", Md);
            model.addAttribute("tituloOriginal", titulo);
            return "form";
        }

        // Limpiar el nombre del archivo para evitar problemas de seguridad
        titulo = titulo.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        if (!titulo.endsWith(".md")) {
            titulo += ".md";
        }

        try {
            Path path = Paths.get(CARPETA_NOTAS, titulo);

            // Si estamos renombrando, eliminar el archivo anterior
            if (tituloAnterior != null && !tituloAnterior.equals(titulo)) {
                Files.deleteIfExists(Paths.get(CARPETA_NOTAS, tituloAnterior));
            }

            Files.writeString(path, Md);

        } catch (IOException e) {
            model.addAttribute("errorG", "Ha ocurrido un error al guardar el archivo.");
            model.addAttribute("textoOriginal", Md);
            model.addAttribute("tituloOriginal", titulo);
            return "form";
        }

        return "redirect:/";
    }

    @GetMapping("/nota/ver")
    public String ver(@RequestParam String archivo, Model model) {
        try {
            Path path = Paths.get(CARPETA_NOTAS, archivo);

            if (Files.notExists(path)) {
                model.addAttribute("error", "El archivo no existe");
                return "visor";
            }

            String contenido = Files.readString(path);

            Parser parser = Parser.builder().build();
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String html = renderer.render(parser.parse(contenido));

            model.addAttribute("completado", true);
            model.addAttribute("contenido", html);
            model.addAttribute("archivo", archivo);

        } catch (IOException e) {
            model.addAttribute("error", "Error al leer el archivo");
        }
        return "visor";
    }

    @PostMapping("/nota/eliminar")
    public String eliminar(@RequestParam String archivo) {
        try {
            Path path = Paths.get(CARPETA_NOTAS, archivo);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Manejar error si es necesario
        }
        return "redirect:/";
    }
}

