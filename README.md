# COMMENTS

## Controllers
### CommentsAdminController ```@RequestMapping(/admin/comments)```
* ```getComments(Integer size(default 10), Integer from)``` - получение всех комментариев
* ```deleteComment(Long commentId)``` - удаление комментария по его id
---
### CommentsPrivateController ``@RequestMapping(/events/{eventId}/comments)``
* ```createComment(CommentCreateDto commentCreateDto, Long userId, Long eventId)``` - создание комментария автоматизированным пользователем 
* ```getAllCommentsByUserId(Long userId)``` - получение всех комментариев пользователя (ПРИШЛОСЬ ОТКАЗАТЬСЯ)
* ```updateComment(Long userId, CommentUpdateDto commentUpdateDto, Long commentId)``` - обновление комментария 
* ```deleteComment(Long userId, Long eventId, Long commentId)``` - удаление комментария
---
### CommentsPublicController ```@RequestMapping(events/{eventId}/comments)```
* ```getCommentsByEventId(Integer from, Integer size, Long eventId)``` - получение определенного кол-во комментариев
---
### EventPublicController
* ```getEventsWithComments(Long eventId)``` - возвращает event с тремя самыми свежими (по дате создания) комментариями
