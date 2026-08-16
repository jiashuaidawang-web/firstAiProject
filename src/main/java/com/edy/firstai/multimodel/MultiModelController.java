package com.edy.firstai.multimodel;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/multi")
public class MultiModelController {

    private final MultiModelChatService multiModelChatService;
    private final ModelRouter modelRouter;

    public MultiModelController(MultiModelChatService multiModelChatService, ModelRouter modelRouter) {
        this.multiModelChatService = multiModelChatService;
        this.modelRouter = modelRouter;
    }

    @GetMapping("/models")
    public Set<String> models() {
        return modelRouter.availableModels();
    }

    @GetMapping("/chat")
    public String chat(
            @RequestParam String model,
            @RequestParam String prompt) {
        return multiModelChatService.chatWithModel(model, prompt);
    }

    @GetMapping("/smart")
    public String smart(@RequestParam String prompt) {
        return multiModelChatService.smartChat(prompt);
    }

    @GetMapping("/compare")
    public Map<String, String> compare(@RequestParam String prompt) {
        return multiModelChatService.compareModels(prompt);
    }
}
