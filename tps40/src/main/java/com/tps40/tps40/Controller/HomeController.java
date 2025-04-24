package com.tps40.tps40.Controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {


    @GetMapping("/")
    public String mostrarHome() {
        return "index";
    }

    @GetMapping("/home")
    public String mostrarHomeAlternativo() {
        return "index";
    }

}
