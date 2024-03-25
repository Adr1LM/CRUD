package com.paellasoft.CRUD.service;

import com.paellasoft.CRUD.chatGpt.ChatGptRequest;
import com.paellasoft.CRUD.chatGpt.ChatGptResponse;
import com.paellasoft.CRUD.entity.Boe;
import com.paellasoft.CRUD.entity.BoeUser;
import com.paellasoft.CRUD.entity.User;
import com.paellasoft.CRUD.mail.EmailSender;
import com.paellasoft.CRUD.repository.IBoeRepository;
import com.paellasoft.CRUD.repository.IBoeUser;
import com.paellasoft.CRUD.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Component
@Transactional
public class BoeService {
    @Value("${openai.api.key}")
    private String OPENAI_API_KEY;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    @Autowired
    private RestTemplate template;

    @Autowired
    private IBoeRepository boeRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IBoeUser boeUserRepo;
    @Autowired
    private EmailSender emailSender;

    @Autowired
    private UserService userService;




    @Scheduled(cron = "0 * * * * *")
    public String obtenerBoeDelDia() {

        // Obtener la fecha actual
        LocalDate fechaActual = LocalDate.now();
        // Formatear la fecha en el formato esperado por la URL del BOE
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String fechaFormateada = fechaActual.format(formatter);
        // Construir la URL del BOE del día actual
        String url = "https://www.boe.es/boe/dias/" + fechaFormateada + "/index.php?s=1";
        // Crear cliente HTTP
        HttpClient client = HttpClient.newHttpClient();
        // Crear solicitud HTTP GET para obtener el BOE
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        try {
            // Enviar solicitud y obtener respuesta
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // Verificar si la solicitud fue exitosa (código de estado 200)
            if (response.statusCode() == 200) {
                // Extraer el contenido HTML del BOE
                String boeContent = response.body();
                String htmlContent = response.body();
                // Procesar HTML para extraer texto puro
                String textoPuro = extraerTextoPuro(htmlContent);

                //-------comprobar si ya esta registrado el Boe del día
               comprobarCambiosEnBoe(textoPuro);



                return textoPuro;

            } else {
                // Manejar errores de solicitud HTTP
                System.out.println("Error al obtener el BOE del día: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }
    }




    public void comprobarCambiosEnBoe(String textoPuro) {

        System.out.println(textoPuro);

        String fragmentoTextoOriginal = textoPuro.substring(5, 40);

        String trampa = "trampa";

        //fragmentoTextoOriginal=trampa;



       // fragmentoTextoOriginal="trampa";

        Boe ultimoBoe = boeRepository.findTopByOrderByFechaBoeDesc();

        if(ultimoBoe==null){

            registrarBoe(textoPuro);

            System.out.println("ultimo boe es null");

        }
       else {


            //comprobar contenido del ultimo boe guardado con el obtenido ahora
            if (fragmentoTextoOriginal.equals(ultimoBoe.getContenidoOriginal())) {
                System.out.println("Este boe ya esta registrado");
            } else {

                registrarBoe(textoPuro);
            }

        }



    }


    private void registrarBoe(String textoPuro){

        try {
            String resumen = resumirConChatGpt(textoPuro);
            String fragmentoTextoOriginal = textoPuro.substring(5, 40);


            System.out.println(fragmentoTextoOriginal);

            String fragmentoResumen = resumen.substring(5, 40);
            //Fecha y hora para registro del Boe
            DateTimeFormatter formateoRegistro = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss");
            LocalDateTime fecha = LocalDateTime.now();
            String fechaRegistro = fecha.format(formateoRegistro);


            System.out.println(fragmentoResumen);
            System.out.println(fechaRegistro);


            String trampa = "trampa";

            //fragmentoTextoOriginal=trampa;

            // Crear el objeto Boe
            Boe boe = new Boe();
            boe.setContenidoOriginal(fragmentoTextoOriginal);
            boe.setContenidoResumido(resumen);
            boe.setFechaBoe(fechaRegistro);

            boeRepository.save(boe);

            notificarSubscriptores(resumen);


        }catch (Exception e){
           e.printStackTrace();
        }

    }


    private void notificarSubscriptores(String resumen){

        List<User> usuarios = userRepository.findAll();

        for(User user:usuarios){

            if(user.isSendNotification()){

                // Envío de correo electrónico de confirmación
                String to = user.getEmail();
                String subject = "Confirmación de registro";
                String text = "Hola " + user.getUsername() + ", hay un nuevo Boe:\n "+resumen;
                emailSender.sendEmail(to, subject, text);
            }
        }
    }

    private String extraerTextoPuro(String htmlContent) {
        // Parsear el contenido HTML utilizando Jsoup
        Document doc = Jsoup.parse(htmlContent);

        // Extraer el texto de todas las etiquetas <p> (párrafos) y <div> (divisiones)
        Element elementosTexto = doc.selectFirst("div.sumario");

        //Elements elementosTexto = doc.select(".sumario");

        // Element elementosTexto = doc.getElementById("sec661");
        String texto = elementosTexto.text();

        // Limitar la cantidad de texto extraído
        int maxTokens = 16385; // Establecer el límite máximo de tokens permitidos
        if (texto.length() > maxTokens) {
            texto = texto.substring(0, maxTokens);    }

        return texto;
    }




    private String resumirConChatGpt(String texto) {
        try {
            // Crear la solicitud a la API de OpenAI
            ChatGptRequest request = new ChatGptRequest(model, "Resume a la mitad lo destacable: "+ texto);

            // Realizar la solicitud a la API de OpenAI
            ChatGptResponse response = template.postForObject(apiUrl, request, ChatGptResponse.class);

            // Extraer el resumen del texto de la respuesta
            String resumen = response.getChoices().get(0).getMessage().getContent();

            // Retornar el resumen
            return resumen;
        } catch (Exception e) {
            // Manejar excepciones
            e.printStackTrace();
            return null;
        }
    }

    public void deleteAllBoes(){
        boeRepository.deleteAll();
    }









}
