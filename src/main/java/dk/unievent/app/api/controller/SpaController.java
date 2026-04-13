package dk.unievent.app.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Catch-all controller for SPA (Single Page Application) routing.
 * Forwards any non-API request without a file extension to index.html,
 * allowing the frontend router to handle the navigation.
 */
@Controller
public class SpaController {

    @GetMapping(value = {"/{path:[^.]*}", "/**/{path:[^.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
