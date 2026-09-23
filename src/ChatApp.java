/**
 * Puente de compatibilidad con versiones anteriores.
 * Redirige la ejecución hacia el nuevo punto de entrada modular y escalable com.chatlocal.Main.
 */
public class ChatApp {

    public static void main(String[] args) {
        com.chatlocal.Main.main(args);
    }
}
