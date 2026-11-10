package com.lexify.service;

import com.lexify.dto.StatisticsResponse;
import com.lexify.repository.MessageRepository;
import com.lexify.repository.RoomRepository;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final UserRepository users;
    private final RoomRepository rooms;
    private final MessageRepository messages;
    private final WordRepository words;

    public StatisticsResponse get() {
        try (var userIds = users.streamIds();
             var roomIds = rooms.streamIds();
             var messageIds = messages.streamIds()) {
            var texts = words.findEnglishTexts();
            var uniqueWords = texts.stream()
                    .map(String::strip)
                    .map(text -> text.toLowerCase(Locale.ROOT))
                    .distinct()
                    .count();
            return new StatisticsResponse(userIds.count(), roomIds.count(), messageIds.count(),
                    texts.stream().count(), uniqueWords);
        }
    }
}
