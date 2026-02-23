package com.meet5.socialnetwork.controller;

import com.meet5.socialnetwork.domain.ProfileVisitorView;
import com.meet5.socialnetwork.dto.LikeRequest;
import com.meet5.socialnetwork.dto.InteractionResponse;
import com.meet5.socialnetwork.dto.VisitRequest;
import com.meet5.socialnetwork.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
/**
 * Handles user-to-user interactions such as visits, likes, and visitor reads.
 */
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/visit")
    @ResponseStatus(HttpStatus.CREATED)
    /** Records a profile visit after validation and fraud checks. */
    public InteractionResponse visit(@Valid @RequestBody VisitRequest request) {
        return interactionService.visit(request.visitorId(), request.visitedId());
    }

    @PostMapping("/like")
    @ResponseStatus(HttpStatus.CREATED)
    /** Records a profile like while enforcing duplicate and self-like rules. */
    public InteractionResponse like(@Valid @RequestBody LikeRequest request) {
        return interactionService.like(request.likerId(), request.likedId());
    }

    @GetMapping("/visitors")
    /** Returns visitors for a user in descending recency order with pagination. */
    public List<ProfileVisitorView> visitors(
            @RequestParam long userId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return interactionService.getVisitors(userId, limit, offset);
    }
}
