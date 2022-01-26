public class Token {
    int id;
    int val;
    TokenKind kind;

    public Token(int info, TokenKind kind, boolean isIdentity) {
        if (isIdentity) {
            this.id = info;
        } else {
            this.val = info;
        }
        this.kind = kind;
    }

    public Token() {
        this.id = -1;
    }
}
