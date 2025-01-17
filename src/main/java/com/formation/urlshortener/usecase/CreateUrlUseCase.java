package com.formation.urlshortener.usecase;

import com.formation.urlshortener.bdd.BddRepository;
import com.formation.urlshortener.bdd.UrlEntity;
import com.formation.urlshortener.personalexception.InvalidUrlException;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

@Service
public class CreateUrlUseCase {
    private final BddRepository bddRepository;
    private final Mapper mapper;

    // thread safe ???
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY HH:mm");

    CreateUrlUseCase(BddRepository bddRepository, Mapper mapper) {
        this.bddRepository = bddRepository;
        this.mapper = mapper;
    }

    private final String[] schemes = { "http", "https" };

    public boolean check(URI newUri, String host) throws InvalidUrlException, IOException, InterruptedException {
        return validateUrl(newUri, host) && bddRepository.notExist(newUri) && pingUrl(newUri)
    }

    private boolean validateUrl(URI newUri, String host) throws InvalidUrlException {
        for (String scheme : schemes) {
            if (newUri.getScheme().equals(scheme)) {
                System.out.println("@@@@@@@ le scheme de " + newUri + " est validé");
                System.out.println("@@@ host url : " + newUri.getHost() + ", host local " + host);
                if (host.contains(newUri.getHost())) {
                    System.out.println("@@@@@@@ URL REFUSE");
                    throw new InvalidUrlException();
                }
                return true;
            }
        }
        throw new InvalidUrlException();
    }

    // SSRF (Server Side Request Forgery) ???
    // http://10.x.x.x
    // https://portail.apps.eul.sncf.fr
    private boolean pingUrl(URI newUri) throws IOException, InterruptedException, InvalidUrlException {
        final var request = HttpRequest.newBuilder(newUri)
                .GET()
                .build();
        final var response = HttpClient.newHttpClient()
                .send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 200) {
            System.out.printf("@@@@@@@ Status code %d ", response.statusCode());

            return true;
        }
        throw new InvalidUrlException();
    }

    public HashMap<String, Object> createUrlEntity(URI newUrl) throws IOException {
        System.out.println("@@@@@@@ ma fonction createUrlEntity");
        UrlEntity newEntity = new UrlEntity(UUID.randomUUID().toString(),
                genString(8),
                newUrl,
                genString(40),
                new SimpleDateFormat("dd-MM-YYYY HH:mm").format(new Date()));

        System.out.println("@@@@@@@ ma nouvelle entity " + newEntity);

        bddRepository.addUrl(newEntity);

        HashMap<String, Object> elemToSend = new HashMap<>();
        elemToSend.put("entity", entityDto(newEntity));
        elemToSend.put("token", newEntity.getToken());
        System.out.println("@@@@@@@ mon token envoyé : " + elemToSend.get("token"));

        return elemToSend;
    }

    // generation d'une suite de caractere alpohanumerique aléatoire
    // d'une longueur donnée
    public static String genString(int size) {
        char leftLimit = '0'; // numeral '0'
        char rightLimit = 'z'; // letter 'z'
        SecureRandom random = new SecureRandom(); // un PRNG qui n'est pas cryptographiquement sécurisé. les valeurs sont prédictibles (car basées sur le temps courant).

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= '9' || i >= 'A') && (i <= 'Z' || i >= 'a'))
                .limit(size)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public NewEntityDto entityDto(UrlEntity newUrlEntity) {
        return mapper.toDto(newUrlEntity);

    }
}
