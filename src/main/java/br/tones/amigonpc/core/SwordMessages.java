package br.tones.amigonpc.core;

import java.util.concurrent.ThreadLocalRandom;

public final class SwordMessages {

    private SwordMessages() {}

    private static String pick(String[] arr, int lvl) {
        if (arr == null || arr.length == 0) return null;
        int i = ThreadLocalRandom.current().nextInt(arr.length);
        return arr[i].replace("{lvl}", String.valueOf(lvl));
    }

    public static String randomUp(int lvl)   { return pick(UP, lvl); }
    public static String randomDown(int lvl) { return pick(DOWN, lvl); }
    public static String randomEpic(int lvl) { return pick(EPIC, lvl); }

    // =========================
    // UP (normal)
    // =========================
    private static final String[] UP = new String[] {
        "Espadas +1! Nível {lvl} alcançado.",
        "Treino pago. Agora tô no nível {lvl} de espada.",
        "Mais afiado que ontem. Espadas nível {lvl}.",
        "Eu senti o peso da lâmina mudar… nível {lvl}.",
        "Subi o estilo: Espadas {lvl}.",
        "Se prepara… minha espada evoluiu. {lvl}!",
        "Ok… isso foi progresso real. Nível {lvl}.",
        "Novo marco atingido: Espadas {lvl}.",
        "Corte mais limpo. Nível {lvl} conquistado.",
        "Minha lâmina tá “falando” melhor agora. {lvl}.",
        "Um passo à frente na arte da espada: {lvl}.",
        "Agora eu corto o vento com mais respeito. {lvl}.",
        "Espadas {lvl}. Quem tá contando é você, eu só evoluo.",
        "Mais reflexo, menos erro. Nível {lvl}.",
        "A prática tá virando instinto. Espadas {lvl}.",
        "Subi de nível e nem suei (mentira). {lvl}.",
        "Upgrade confirmado: Espadas {lvl}.",
        "A lâmina não falha… eu também não. {lvl}.",
        "Novo nível desbloqueado: {lvl} em espadas.",
        "Mais precisão. Mais dano. Nível {lvl}.",
        "Essa foi bonita… Espadas {lvl}.",
        "Minhas mãos tão mais rápidas. {lvl}.",
        "Nível {lvl}! Agora eu entendo melhor o timing.",
        "Eu ouvi o “cling” da evolução: {lvl}.",
        "Se eu errar menos, eu venço mais. {lvl}.",
        "Espadas {lvl}. Próximo alvo: ficar imparável.",
        "Treinei. Aprendi. Subi. {lvl}.",
        "Minha postura melhorou. Espadas nível {lvl}.",
        "Aço e disciplina: {lvl}.",
        "Nível {lvl}… a lâmina tá mais confiante que eu.",
        "Subi! Agora eu corto até dúvida. {lvl}.",
        "Espadas {lvl}: o básico ficou fácil.",
        "Agora eu sei exatamente quando avançar. {lvl}.",
        "Mais controle de distância. {lvl}.",
        "Nível {lvl}: a margem de erro diminuiu.",
        "Evolução silenciosa… resultado barulhento. {lvl}.",
        "Espadas {lvl}. Se precisar, eu abro caminho.",
        "Agora eu acompanho melhor teu ritmo. {lvl}.",
        "Nível {lvl}: força sem perder finesse.",
        "Aprendi mais um “segredo” da espada. {lvl}.",
        "Espadas {lvl}. Eu tô ficando perigoso.",
        "Cada batalha ensina. Nível {lvl}.",
        "Mais firmeza no punho. {lvl}.",
        "Nível {lvl}: defesa e ataque mais certos.",
        "Subi. E nem foi sorte. {lvl}.",
        "Espadas {lvl}. Ainda dá pra melhorar muito.",
        "Nível {lvl}: eu tô lendo melhor os movimentos.",
        "A lâmina tá mais leve… ou eu que tô melhor? {lvl}.",
        "Novo nível {lvl}. Próxima luta é minha.",
        "Espadas {lvl}: agora eu não hesito.",
        "Eu evoluí. Você viu? Nível {lvl}.",
        "Mais agressivo, mais inteligente. {lvl}.",
        "Nível {lvl}! Bora testar isso em combate.",
        "Espadas {lvl}. O progresso tá acelerando.",
        "Meu corte ficou mais “cirúrgico”. {lvl}.",
        "Nível {lvl}. Eu tô pronto pra linha de frente.",
        "Espadas {lvl}. Sem drama, só resultado.",
        "Eu melhorei. O inimigo vai notar. {lvl}.",
        "Nível {lvl}: treino + experiência = ameaça.",
        "Espadas {lvl}. Mantém o foco que eu mantenho a lâmina.",
        "Nível {lvl}. Minha espada pediu aumento.",
        "Subi! Agora eu erro menos… eu acho. {lvl}",
        "Espadas {lvl}. Não foi hack, foi treino.",
        "Nível {lvl}. A lâmina tá feliz.",
        "Espadas {lvl}. O inimigo que lute.",
        "Nível {lvl}. Eu tô ficando “cortante” demais.",
        "Espadas {lvl}: desbloqueei o modo sério.",
        "Nível {lvl}. Agora eu corto até conversa fiada.",
        "Espadas {lvl}. Se eu cair, foi lag.",
        "Nível {lvl}. Se prepara que eu tô confiante.",
        "Espadas {lvl}. Treino é treino, loot é loot.",
        "Nível {lvl}. Aço afiado, ego também."
    };

    // =========================
    // EPIC (marco / troca de espada) — usar quando subir e atravessar marco
    // =========================
    private static final String[] EPIC = new String[] {
        "Marco atingido! Espadas {lvl} — a lâmina virou extensão do meu braço.",
        "Nível {lvl}! Agora eu luto com calma… e acerto com certeza.",
        "Espadas {lvl}. A próxima batalha vai lembrar meu nome.",
        "Aço, honra e prática. Cheguei no {lvl}.",
        "Nível {lvl}: eu não treino mais… eu aperfeiçoo.",
        "Espadas {lvl}. O básico ficou passado.",
        "Nível {lvl}! Minha guarda tá sólida como pedra.",
        "Espadas {lvl}: hoje eu corto o impossível em pedaços.",
        "Nível {lvl}. A lâmina responde antes do pensamento.",
        "Espadas {lvl}. Agora eu sou a linha de frente.",
        "Nível {lvl}: precisão que dá medo.",
        "Espadas {lvl}. Quem avançar, cai.",
        "Nível {lvl}. Agora eu controlo o ritmo do combate.",
        "Espadas {lvl}. Eu evoluí — e não foi pouco.",
        "Nível {lvl}. Hora de subir a dificuldade.",
        "Espadas {lvl}: disciplina virou poder.",
        "Nível {lvl}. O aço tá cantando vitória.",
        "Espadas {lvl}. Eu tô pronto pro que vier.",
        "Nível {lvl}: hoje eu luto como veterano.",
        "Espadas {lvl}. A lenda começa aqui."
    };

    // =========================
    // DOWN
    // =========================
    private static final String[] DOWN = new String[] {
        "Perdi experiência. Espadas voltou para o nível {lvl}.",
        "Nível reduzido. Agora estou no {lvl} em espadas.",
        "Queda registrada. Espadas desceu para {lvl}.",
        "Fui penalizado. Espadas agora é {lvl}.",
        "Voltei um passo. Espadas nível {lvl}.",
        "Perdi ritmo. Espadas caiu para {lvl}.",
        "Erro caro. Espadas retornou ao nível {lvl}.",
        "Não foi dessa vez. Espadas desceu para {lvl}.",
        "A derrota cobrou o preço. Espadas {lvl}.",
        "Recuo necessário. Espadas nível {lvl}.",
        "Me custou caro. Agora estou no {lvl} em espadas.",
        "Perdi nível, mas não perdi a vontade. Espadas {lvl}.",
        "Espadas caiu para {lvl}. Vou recuperar.",
        "Nível {lvl}. Ajustando postura e aprendendo com o erro.",
        "Voltei para {lvl}. Da próxima eu faço melhor.",
        "Espadas {lvl}. Hora de voltar ao básico e corrigir.",
        "Perdi consistência. Espadas desceu para {lvl}.",
        "O progresso diminuiu. Espadas está em {lvl}.",
        "Nível {lvl}. Preciso melhorar minha execução.",
        "Espadas {lvl}. Vou compensar no próximo combate.",
        "Fui punido pela falha. Espadas {lvl}.",
        "Nível caiu para {lvl}. Vou focar mais.",
        "A lâmina ficou mais pesada hoje. Espadas {lvl}.",
        "Espadas voltou para {lvl}. Retomando controle.",
        "Voltei para {lvl}. Ainda dá pra evoluir de novo.",
        "Nível {lvl}. Reavaliando meu timing.",
        "Espadas {lvl}. Menos pressa, mais precisão.",
        "Cai para {lvl}. Vou recuperar com treino.",
        "Perdi nível. Vou recuperar com disciplina. Espadas {lvl}.",
        "Espadas {lvl}. Sem desculpas, só ajuste.",
        "Nível {lvl}. Preciso escolher melhor as trocas.",
        "Espadas desceu para {lvl}. Vou voltar mais forte.",
        "Desci para {lvl}. Agora é reconstruir.",
        "O golpe falhou e eu paguei. Espadas {lvl}.",
        "Nível {lvl}. Hora de jogar mais seguro.",
        "Espadas {lvl}. Vou cuidar melhor da minha defesa.",
        "Caiu para {lvl}. A lição foi aprendida.",
        "Perdi nível. Ainda tenho caminho. Espadas {lvl}.",
        "Espadas {lvl}. Vou refazer o caminho com calma.",
        "Nível {lvl}. A base precisa ficar sólida.",
        "Recuo temporário. Espadas {lvl}.",
        "Espadas {lvl}. Vou recuperar com consistência.",
        "Nível {lvl}. Vou treinar até isso virar instinto.",
        "Perdi nível. O foco continua. Espadas {lvl}.",
        "Espadas caiu para {lvl}. Vou recuperar na prática.",
        "Nível {lvl}. Hora de voltar a vencer no detalhe.",
        "Voltei para {lvl}. Vou provar que foi só um tropeço.",
        "Espadas {lvl}. Vou corrigir meus erros de distância.",
        "Nível {lvl}. Vou melhorar minha leitura de movimento.",
        "Espadas {lvl}. Não acabou, só atrasou.",
        "Desci para {lvl}. Recomeço com cabeça fria.",
        "Nível {lvl}. Mantendo a calma e reconstruindo.",
        "Espadas {lvl}. Eu ainda vou voltar ao topo.",
        "Perdi nível. Mas ganhei aprendizado. Espadas {lvl}.",
        "Espadas {lvl}. Vou recuperar com trabalho limpo.",
        "Nível {lvl}. Ajuste feito, próxima luta é diferente.",
        "Voltei ao {lvl}. Hora de retomar o controle.",
        "Espadas {lvl}. Recuperação começa agora.",
        "Nível {lvl}. Vou focar em consistência.",
        "Espadas {lvl}. Vou voltar a acertar mais do que erro.",
        "Desci para {lvl}. Vou refazer esse caminho melhor.",
        "Nível {lvl}. Vou treinar até isso não acontecer de novo.",
        "Espadas {lvl}. Falhei, aprendi, vou recuperar.",
        "Voltei para {lvl}. Eu sei como subir de novo.",
        "Espadas {lvl}. O plano continua, só mudou o passo.",
        "Nível {lvl}. Hora de parar de arriscar sem necessidade.",
        "Espadas {lvl}. Vou lutar com mais cabeça."
    };
}
