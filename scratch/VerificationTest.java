import com.chatlocal.backend.model.*;
import com.chatlocal.backend.service.AudioRecorderService;
import com.chatlocal.backend.service.ChatServiceImpl;

import java.io.File;
import java.util.List;

public class VerificationTest {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBAS DE VERIFICACIÓN ===");

        // 1. AudioRecorderService
        AudioRecorderService audioService = new AudioRecorderService();
        System.out.println("AudioRecorderService instanciado: OK");

        // 2. ChatMessage & Status & Private vs Room
        ChatMessage msg1 = ChatMessage.createTextMessage("Alice", 0x8B5CF6, "Hola Bob", true, "general");
        if (msg1.getStatus() != MessageStatus.SENT) throw new AssertionError("Status inicial debe ser SENT");
        msg1.setStatus(MessageStatus.DELIVERED);
        if (msg1.getStatus() != MessageStatus.DELIVERED) throw new AssertionError("Status actualizado debe ser DELIVERED");

        ChatMessage privMsg = new ChatMessage("msg-123", "Alice", 0x8B5CF6, "Mensaje secreto", MessageType.TEXT, true, null, 0, null, 0, "private", null, "Bob");
        if (!privMsg.isPrivate()) throw new AssertionError("isPrivate debe ser true");
        if (!"Bob".equals(privMsg.getRecipient())) throw new AssertionError("Recipient incorrecto");
        System.out.println("ChatMessage (Estados, checks y privacidad): OK");

        // 3. ChatServiceImpl aislamiento de conversaciones
        ChatServiceImpl service = new ChatServiceImpl();
        UserProfile bob = new UserProfile("Bob", ConnectionRole.CLIENT, "192.168.1.10", 5000);
        service.setActivePrivateUser(bob);
        if (!"private_Bob".equals(service.getActiveConversationId())) throw new AssertionError("activeConversationId debe ser private_Bob");

        service.storeMessage("private_Bob", privMsg);
        service.storeMessage("room_general", msg1);

        List<ChatMessage> bobHistory = service.getConversationMessages("private_Bob");
        List<ChatMessage> genHistory = service.getConversationMessages("room_general");

        if (bobHistory.size() != 1 || !bobHistory.get(0).getContent().equals("Mensaje secreto")) {
            throw new AssertionError("Fallo en historial privado de Bob");
        }
        if (genHistory.size() != 1 || !genHistory.get(0).getContent().equals("Hola Bob")) {
            throw new AssertionError("Fallo en historial de Sala General");
        }
        System.out.println("ChatServiceImpl aislamiento de conversaciones (1-a-1 vs Salas): OK");

        // 4. Wallpaper path
        service.setChatWallpaperPath("C:\\test\\wallpaper.jpg");
        if (!"C:\\test\\wallpaper.jpg".equals(service.getChatWallpaperPath())) throw new AssertionError("Fallo en Wallpaper path");
        System.out.println("Chat Wallpaper setting: OK");

        System.out.println("=== TODAS LAS PRUEBAS PASARON SATISFACTORIAMENTE ===");
    }
}
