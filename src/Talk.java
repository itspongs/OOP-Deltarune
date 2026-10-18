/** Seam's talk topics, each with pages of dialogue. */
public class Talk {
    private final String[] topics = {"About yourself", "About Sir Gudo", "About Reynold"};
    private final String[][] lines = {
        {
            "* I am Seam, a darkner in the realms of the Dark World.",
            "* Just here having my business. Come, Lightner and have a taste of our fine cuisine.",
            "* That's all me, fellow lightner!"
        },
        {
            "* Sir Gudo?",
            "* He's a great professor, especially with his code fricking tracings.",
            "* Back in my days, I looked up to him because I love his cute smile."
        },
        {
            "* Reynold, huh?",
            "* Well, on my end...",
            "* lowkey not gonna lie, jit trippin he is super gay and loves to kiss his pet dawg."
        }
    };
    public String[] getTopics() { return topics.clone(); }
    public int size() { return topics.length; }
    public int pages(int i) {
        if (i < 0 || i >= lines.length) return 1;
        return lines[i].length;
    }
    public String lineFor(int i, int page) {
        if (i < 0 || i >= lines.length) return "* ...";
        if (page < 0 || page >= lines[i].length) return "* ...";
        return lines[i][page];
    }
}
