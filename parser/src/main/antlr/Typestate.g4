grammar Typestate;

@header {
    import ast.*;
    import static ast.Utils.map;
    import static ast.Position.fromToken;
    import static ast.Position.fromTokens;
}

typestate returns [TypeStateNode node]:
    t=TYPESTATE name=ID '{' sts+=state* '}' EOF
    {
        $node = new TypeStateNode(
            fromToken($t),
            new IdNode(fromToken($name), $name.getText()),
            map($sts, s -> s.node)
        );
    }
;

state returns [StateNode node] locals [boolean droppable]:
    name=ID '=' '{' ts+=transition (',' ts+=transition)* (',' DROP ':' END {$droppable=true;})? '}'
    {
        $node = new StateNode(
            fromToken($name),
            new IdNode(fromToken($name), $name.getText()),
            map($ts, t -> t.node),
            $droppable
        );
    }
;

transition returns [TransitionNode node]:
    m=method ':' t=target
    {$node = new TransitionNode($m.node.getPosition(), $m.node, $t.node);}
;

target returns [TargetNode node]:
    id=ID {$node = new StateRefNode(fromToken($id), new IdNode(fromToken($id), $id.getText()));}
    | e=END {$node = new EndStateNode(fromToken($e));}
    | t='<' bs+=branch (',' bs+=branch)+ '>'
    {
        $node = new DecisionTargetNode(
            Position.fromToken($t),
            map($bs, b -> b.node)
        );
    }
;

method returns [MethodNode node]:
    name=ID '(' (args+=type (',' args+=type)*)? ')'
    {
        $node = new MethodNode(
            fromToken($name),
            new IdNode(fromToken($name), $name.getText()),
            map($args, a -> a.node)
        );
    }
;

branch returns [BranchNode node]:
    label=ID ':' (
        id=ID
        {
            $node = new BranchNode(
                fromToken($label),
                new IdNode(fromToken($label), $label.getText()),
                new StateRefNode(fromToken($id),new IdNode(fromToken($id), $id.getText()))
            );
        }
        | e=END
        {
            $node = new BranchNode(
                fromToken($label),
                new IdNode(fromToken($label), $label.getText()),
                new EndStateNode(fromToken($e))
            );
        }
    )
;

type returns [TypeNode node]:
    ids+=ID ('.' ids+=ID)* (arrs+='[]')*
    {
        $node = new TypeNode(
            fromToken($ids.get(0)),
            map($ids, i -> new IdNode(fromToken(i), i.getText())),
            $arrs.size()
        );
    }
;

// keywords
TYPESTATE : 'typestate';
DROP : 'drop' ;
END : 'end' ;

// identifiers
ID : [$_a-zA-Z]+[$_a-zA-Z0-9]* ;

// skip
WS : [ \t\r\n]+ -> skip ;
BlockComment : '/*' .*? '*/' -> skip ;
LineComment : '//' ~[\r\n]* -> skip ;