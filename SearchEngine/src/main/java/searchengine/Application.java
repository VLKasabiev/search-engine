package searchengine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import searchengine.config.ConnectSettings;

@SpringBootApplication
public class Application{
    @Autowired
    private ConnectSettings connectSettings;
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

    }
}
