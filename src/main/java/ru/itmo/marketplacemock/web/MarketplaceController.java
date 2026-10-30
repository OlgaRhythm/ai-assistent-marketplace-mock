package ru.itmo.marketplacemock.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MarketplaceController {

    private final String assistantFrontendUrl;

    public MarketplaceController(@Value("${assistant.frontend-url}") String assistantFrontendUrl) {
        this.assistantFrontendUrl = assistantFrontendUrl;
    }

    @GetMapping("/")
    public String marketplace(Model model) {
        model.addAttribute("assistantFrontendUrl", assistantFrontendUrl);
        return "marketplace";
    }
}
