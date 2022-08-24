package Scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Scanner.TokenType.*; // [static-import]

class Scanner {
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();

    // Values
    keywords.put("true", TRUE);
    keywords.put("false",  FALSE);
    keywords.put("null", NULL);

    // Boolean Operators
    keywords.put("and",    AND);
    keywords.put("or",     OR);
    keywords.put("xor",     XOR);
    keywords.put("not", NOT);
    keywords.put("implies", IMPLIES);

    // Classes
    keywords.put("model", MODEL);
    keywords.put("class",  CLASS);
    keywords.put("attributes", ATTRIBUTES);
    keywords.put("operations", OPERATIONS);

    // State machines
    keywords.put("statemachines", STATE_MACHINES);
    keywords.put("initial", INITIAL);
    keywords.put("final", FINAL);

    // Associations
    keywords.put("association", ASSOCIATION);
    keywords.put("associationclass", ASSOCIATION_CLASS);
    keywords.put("aggregarion", AGGREGATION);
    keywords.put("composition", COMPOSITION);

    // OCL
    keywords.put("self", SELF);
    keywords.put("begin", BEGIN);
    keywords.put("end", END);
    keywords.put("init", INIT);
    keywords.put("derived", DERIVED);
    keywords.put("context", CONTEXT);
    keywords.put("inv", INV);
    keywords.put("pre", PRE);
    keywords.put("@pre", AT_SYMBOL_PRE);
    keywords.put("post", POST);
    keywords.put("between", BETWEEN);
    keywords.put("if",     IF);
    keywords.put("then", THEN);
    keywords.put("else",   ELSE);
    keywords.put("endif", END_IF);
    keywords.put("result", RESULT);

    // SOIL
    keywords.put("new", NEW);
    keywords.put("destroy", DESTROY);
    keywords.put("insert", INSERT);
    keywords.put("delete", DELETE);
    keywords.put("into", INTO);
    keywords.put("from", FROM);
    keywords.put("declare", DECLARE);
    keywords.put("for", FOR);
    keywords.put("in", IN);
    keywords.put("do", DO);

    // Number Operations
    keywords.put("mod", MOD);
    keywords.put("div", DIV);
    keywords.put("abs", ABS);
    keywords.put("max", MAX);
    keywords.put("min", MIN);
    keywords.put("round", ROUND);
    keywords.put("floor", FLOOR);

    // String Operations
    keywords.put("concat", CONCAT);
    keywords.put("toLower", TO_LOWER);
    keywords.put("toUpper", TO_UPPER);
    keywords.put("substring", SUBSTRING);

    // OclAny Operations
    keywords.put("oclIsNew", OCL_IS_NEW);
    keywords.put("oclIsUndefined", OCL_IS_UNDEFINED);
    keywords.put("oclAsType", OCL_AS_TYPE);
    keywords.put("oclIsTypeOf", OCL_IS_TYPE_OF);
    keywords.put("oclIsKindOf", OCL_IS_KIND_OF);
    keywords.put("oclIsInState", OCL_IS_IN_STATE);
    keywords.put("allInstances", ALL_INSTANCES);

    // OclMessage Operations
    keywords.put("hasRetuned", HAS_RETURNED);
    keywords.put("isSignalSent", IS_SIGNAL_SENT);
    keywords.put("isOperationCall", IS_OPERATION_CALL);

    // Standard Operations
    keywords.put("count", COUNT);
    keywords.put("excludes", EXCLUDES);
    keywords.put("excludesAll", EXCLUDES_ALL);
    keywords.put("includes", INCLUDES);
    keywords.put("includesAll", INCLUDES_ALL);
    keywords.put("isEmpty", IS_EMPTY);
    keywords.put("notEmpty", NOT_EMPTY);
    keywords.put("size", SIZE);
    keywords.put("sum", SUM);

    // Iteration Operations
    keywords.put("any", ANY);
    keywords.put("collect", COLLECT);
    keywords.put("collectNested", COLLECT_NESTED);
    keywords.put("exists", EXISTS);
    keywords.put("forAll", FOR_ALL);
    keywords.put("isUnique", IS_UNIQUE);
    keywords.put("iterate", ITERATE);
    keywords.put("one", ONE);
    keywords.put("reject", REJECT);
    keywords.put("select", SELECT);
    keywords.put("sortedBy", SORTED_BY);

  }
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;

  Scanner(String source) {
    this.source = source;
  }
  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }
  private void scanToken() {
    char c = peek();
    advance();
    switch (c) {
      // Single character
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case '[': addToken(LEFT_SQUARE_BRACKET);
      case ']': addToken(RIGHT_SQUARE_BRACKET);
      case '=': addToken(EQUAL); break;
      case ',': addToken(COMMA); break;
      case '+': addToken(PLUS); break;
      case '*': addToken(STAR); break;
      case ';': addToken(SEMICOLON); break;

      // One or two characters
      case '/':
        if (match('*')) {
          // A comment goes until the end of the line.
          while (!endOfTheComment()) advance();
        } else {
          addToken(SLASH);
        }
        break;
      case '-':
        if(peek() == '-') {
          while (!endOfTheLine()) advance();
          advance();
        } else if (peek() == '>') {
          addToken(MINUS_GRATER);
          advance();
        }else {
          addToken(MINUS);
        }
        break;
      case '<':
        if(peek() == '=') {
          addToken(LESS_EQUAL);
          advance();
        } else if (peek() == '>') {
          addToken(LESS_GRATER);
          advance();
        }else {
          addToken(LESS);
        }
        break;
      case ':':
        if(peek() == ':') {
          addToken(COLON_COLON);
          advance();
        } else if (peek() == '=') {
          addToken(COLON_EQUAL);
          advance();
        }else {
          addToken(COLON);
        }
        break;

      case '.':
        addToken(match('.') ? DOT_DOT : DOT);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;


      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      case '\n':
        line++;
        break;

      case '"': string(); break;

      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Lox.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) type = IDENTIFIER;
    addToken(type);
  }
  private void number() {
    while (isDigit(peek())) advance();

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigit(peek())) advance();
    }

    addToken(NUMBER,
        Double.parseDouble(source.substring(start, current)));
  }
  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    // The closing ".
    advance();

    // Trim the surrounding quotes.
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }
  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }
  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }
  private void advance() {
    current++;
  }
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }
  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }
  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private boolean endOfTheLine() {
    return peek() != '\n';
  }
  private boolean endOfTheComment() {
    return peek() == '*' && peekNext() == '/';
  }
}
