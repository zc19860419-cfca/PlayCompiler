package play.compiler.script.compile;

import play.compiler.script.generate.PlayScriptParser;
import play.compiler.utils.Args;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author zhangchong
 * @CodeReviewer zhangqingan
 * @Description 命名空间
 */
public class NameSpace extends BlockScope {
    private NameSpace parent = null;
    private List<NameSpace> subNameSpaces = new LinkedList<>();

    protected NameSpace(String name, Scope enclosingScope, PlayScriptParser.ProgContext ctx) {
        this.name = name;
        this.enclosingScope = enclosingScope;
        this.ctx = ctx;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 复制一份不可变的子命名空间集合
     *
     * @return
     */
    public List<NameSpace> subNameSpaces() {
        return Collections.unmodifiableList(subNameSpaces);
    }

    public void addSubNameSpace(NameSpace child) {
        Args.notNull(child, "NameSpace#addSubNameSpace:child");
        child.parent = this;
        subNameSpaces.add(child);
    }

    public void removeSubNameSpace(NameSpace child) {
        Args.notNull(child, "NameSpace#removeSubNameSpace:child");
        child.parent = null;
        subNameSpaces.remove(child);
    }
}
