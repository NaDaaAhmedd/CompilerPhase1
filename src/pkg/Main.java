package pkg;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Main {
    HashMap<String,NFA> reg_exp = new HashMap<>();
    HashMap<String,NFA> reg_def = new HashMap<>();
    HashMap<String,Integer> priorities = new HashMap<>();
    HashMap<String,Integer> opPriorities = new HashMap<>();
    HashSet<Character> alphabet = new HashSet<>();
    HashMap < NFA_State,String > acceptence ;

    public static void main(String[] args) {
        Main m = new Main();
        File file = new File("input.txt");
        m.parse_file(file);
    }

    public void parse_file(File file) {
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        opPriorities.put("(", 0);
        opPriorities.put("|", 1);
        opPriorities.put(" ", 2);
        opPriorities.put("*", 3);
        opPriorities.put("+", 3);
        int highestPrioritySoFar = 0;
        while (sc.hasNextLine()) {
            String str = sc.nextLine();
            str = removeSpaces(str);
            String[] words = str.split(" ");
            HashMap<String,NFA> tmp = new HashMap<>();
            splitt(words, tmp);
            if (words[0].charAt(words[0].length() - 1) == ':') {
                str = words[0].substring(0, words[0].length() - 1);
                reg_exp.put(str, regDefExpStack(words, tmp, false));
                priorities.put(str, ++highestPrioritySoFar);
            } else if (words[1].equals("=")) {
                reg_def.put(words[0], regDefExpStack(words, tmp, true));
            } else {
                if (words[0].charAt(0) == '{') {
                    Keypunct(words, true, highestPrioritySoFar);
                } else if (words[0].charAt(0) == '[') {
                    Keypunct(words, false, highestPrioritySoFar);
                }
            }
        }
    }

    private NFA regDefExpStack(String[] words, HashMap<String,NFA> tmp, boolean def) {
        Stack<NFA> nfas = new Stack<>();
        Stack<String> op = new Stack<>();
        int i = def ? 2 : 1;
        int st = i;
        for (; i < words.length; i++) {
            if ((tmp.get(words[i]) != null || words[i].equals("("))
                    && i - 1 >= st && !words[i - 1].equals("|") && !words[i - 1].equals("(")) {
                while (!op.isEmpty() && opPriorities.get(op.peek()) > opPriorities.get(" ")) {
                    compOp(nfas, op);
                }
                op.push(" ");
                if (!words[i].equals("(")) nfas.push(tmp.get(words[i]));
                else op.push("(");
            } else if (words[i].equals("|")) {
                while (!op.isEmpty() && opPriorities.get(op.peek()) > opPriorities.get("|")) {
                    compOp(nfas, op);
                }
                op.push("|");
            } else if (words[i].equals("*") || words[i].equals("+") || words[i].equals("(")) {
                op.push(words[i]);
            } else if (words[i].equals(")")) {
                while (!op.peek().equals("(")) {
                    compOp(nfas, op);
                }
                op.pop();
            } else if (tmp.get(words[i]) != null) nfas.push(tmp.get(words[i]));
        }
        while (!op.isEmpty()) {
            compOp(nfas, op);
        }
        return nfas.pop();
    }

    private void compOp(Stack<NFA> nfas, Stack<String> op) {
        switch (op.peek()) {
            case "*":
                nfas.push(kleene_closure(nfas.pop()));
                op.pop();
                break;
            case "+":
                nfas.push(positive_closure(nfas.pop()));
                op.pop();
                break;
            case " ": {
                NFA sec = nfas.pop();
                NFA fir = nfas.pop();
                nfas.push(and_NFA(fir, sec));
                op.pop();
                break;
            }
            case "|": {
                NFA sec = nfas.pop();
                NFA fir = nfas.pop();
                nfas.push(or_NFA(fir, sec));
                op.pop();
                break;
            }
        }
    }

    private void Keypunct(String[] words, boolean keyword, int highestPriority) {
        for (int i = 1; i < words.length - 1; i++) {
            reg_exp.put(words[i], getNFA(words[i]));
            int put = keyword ? 0 : ++highestPriority;
            priorities.put(words[i], put);
        }
    }

    private void splitt(String[] words, HashMap<String,NFA> tmp) {
        String s;
        for (int i = 1; i < words.length; i++) {
            s = words[i];
            if (!s.equals("(") && !s.equals("|") && !s.equals(")")
                    && !s.equals("*") && !s.equals("+") && !s.equals("{")
                    && !s.equals("}") && !s.equals("[") && !s.equals("]") && !s.equals("=")) {
                tmp.put(s, getNFA(s));
            }
        }
    }

    private NFA getNFA(String str) {
        if (str.length() == 1) {
            alphabet.add(str.charAt(0));
            return convert_to_NFA(str.charAt(0));
        } else if (reg_def.get(str) != null)
            return reg_def.get(str);
        else if (str.contains("-"))
            return fromto(str.indexOf("-"), str);
        else {
            NFA nfa = null;
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == '\\') {
                    if (str.charAt(i + 1) == 'L')
                        nfa = nfa == null ? convert_to_NFA(null) : and_NFA(nfa, convert_to_NFA(null));
                    else {
                        nfa = nfa == null ? convert_to_NFA(str.charAt(i + 1)) : and_NFA(nfa, convert_to_NFA(str.charAt(i + 1)));
                        alphabet.add(str.charAt(i + 1));
                    }
                    i++;
                } else {
                    nfa = nfa == null ? convert_to_NFA(str.charAt(i)) : and_NFA(nfa, convert_to_NFA(str.charAt(i)));
                    alphabet.add(str.charAt(i));
                }
            }
            return nfa;
        }
    }

    public NFA fromto(int i, String word) {
        char from, to;
        from = word.charAt(i - 1);
        to = word.charAt(i + 1);
        NFA nfa = convert_to_NFA(from);
        alphabet.add(from);
        for (char j = (char) (from + 1); j <= to; j++) {
            alphabet.add(j);
            nfa = or_NFA(nfa, convert_to_NFA(j));
        }
        return nfa;
    }

    public String removeSpaces(String s) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            if (s.charAt(i) == ' ') {
                if (i < s.length() - 2 && s.charAt(i + 1) == '-' && s.charAt(i + 2) == ' ') {
                    res.append('-');
                    i += 3;
                } else if (i != 0) {
                    res.append(' ');
                    i++;
                }
                while (i < s.length() && s.charAt(i) == ' ')
                    i++;
            } else {
                boolean check = i + 1 < s.length() && s.charAt(i + 1) != ' ';
                if ((i == 0 || i == s.length() - 1) && (s.charAt(i) == '{' || s.charAt(i) == '['
                        || s.charAt(i) == '}' || s.charAt(i) == ']')) {
                    if (i - 1 >= 0)
                        res.append(' ');
                    res.append(s.charAt(i));
                    if (check) res.append(' ');
                } else if ((s.charAt(i) == '(' || s.charAt(i) == ')' || s.charAt(i) == '*'
                        || s.charAt(i) == '|' || s.charAt(i) == '+') && (i - 1 >= 0 && s.charAt(i - 1) != '\\')) {
                    if (res.charAt(res.length() - 1) != ' ')
                        res.append(' ');
                    res.append(s.charAt(i));
                    if (check) res.append(' ');
                } else
                    res.append(s.charAt(i));
                i++;
            }
        }
        int len = res.length();
        if (res.charAt(len - 1) == ' ') res.delete(len - 1, len);
        return res.toString();
    }



    public NFA convert_to_NFA ( Character s ){

        NFA_State start=new NFA_State();
        NFA_State end=new NFA_State();

        if (s==null)
            start.getEmpty_transitions().add(end);
        else
            start.insert_transition(s,end);

        NFA res=new NFA();

        res.add_accepting_state(end);
        res.getStates().add(end);
        res.getStates().add(start);
        res.setStart_state(start);
        return res;
    }

    public void convert_to_DFA ( NFA nfa ){

    }

    public void minimize_DFA ( DFA dfa){

    }

    public void scan_input(File file , DFA dfa){
        // don't forget symbol table
    }


    public NFA kleene_closure(NFA nfa) {
        /* new start --(^)-> old start done
         * old final --(^)-> old start
         * old final --(^)-> new final
         * new start --(^)-> new final
         * set nfa start to be new start
         * set in nfa accepting states new final
         * remove old final from accepting states
         * set new start and new final in states set
         *  */
        NFA res= deepClone(nfa);
        NFA_State new_start=new NFA_State();
        NFA_State new_final=new NFA_State();
        NFA_State old_start=  res.getStart_state();
        HashSet<NFA_State> accept= res.getAccepting_states();
        NFA_State old_final= accept.iterator().next();
        HashSet<NFA_State> states=res.getStates();

        new_start.getEmpty_transitions().add(old_start);
        old_final.getEmpty_transitions().add(old_start);
        old_final.getEmpty_transitions().add(new_final);
        new_start.getEmpty_transitions().add(new_final);

        res.setStart_state(new_start);
        accept.remove(old_final);
        accept.add(new_final);
        res.setAccepting_states(accept);
        states.add(new_start);
        states.add(new_final);

        return  res;
    }

    public NFA or_NFA (NFA nfa1, NFA nfa2){
        /*
         * new start --(^)->old start1
         * new start --(^)->old start2
         * old final1 --(^)->new final
         * old final2 --(^)->new final
         * create NFA result & set start -->new start
         * move states in nfa1 and nfa2 to result nfa
         * set accepting state --> new final
         * */
        NFA clone_nfa1= deepClone(nfa1);
        NFA clone_nfa2=deepClone(nfa2);

        NFA_State new_start=new NFA_State();
        NFA_State new_final=new NFA_State();

        NFA_State old_start1=clone_nfa1.getStart_state();
        NFA_State old_final1=clone_nfa1.getAccepting_states().iterator().next();
        NFA_State old_start2=clone_nfa2.getStart_state();
        NFA_State old_final2=clone_nfa2.getAccepting_states().iterator().next();

        new_start.getEmpty_transitions().add(old_start1);
        new_start.getEmpty_transitions().add(old_start2);
        old_final1.getEmpty_transitions().add(new_final);
        old_final2.getEmpty_transitions().add(new_final);

        NFA res_nfa=new NFA(new_start);
        HashSet<NFA_State> res_states=res_nfa.getStates();
        res_states.add(new_start);
        res_states.addAll(clone_nfa1.getStates());
        res_states.addAll(clone_nfa2.getStates());
        res_states.add(new_final);
        res_nfa.add_accepting_state(new_final);

        return res_nfa;
    }


    public NFA and_NFA (NFA nfa1, NFA nfa2){

        /*
         *get final of nfa1 (final1)and the start of nfa2(start2)
         * remove the accepting states from nfa1
         * merge the two nfas by make final1 same as start2
         * add states from nfa2 to nfa
         * */
        NFA clone_nfa1= deepClone(nfa1);
        NFA clone_nfa2=deepClone(nfa2);

        NFA_State start2=clone_nfa2.getStart_state();
        NFA_State final1=clone_nfa1.getAccepting_states().iterator().next();

        clone_nfa1.getAccepting_states().remove(final1);
        final1.setTransitions(start2.getTransitions());
        final1.setEmpty_transitions(start2.getEmpty_transitions());
        clone_nfa2.getStates().remove(clone_nfa2.getStart_state());
        clone_nfa1.getStates().addAll(clone_nfa2.getStates());
        clone_nfa1.getAccepting_states().addAll(clone_nfa2.getAccepting_states());

        return clone_nfa1;
    }


    public NFA positive_closure(NFA nfa){
        NFA cloned=deepClone(nfa);
        return and_NFA(cloned,kleene_closure(cloned));
    }

    public NFA combine_NFA(){
        NFA_State start =new NFA_State();
        NFA combined=new NFA(start);
        acceptence=new HashMap<>();
        for (String st:reg_exp.keySet()){
            NFA nfa=reg_exp.get(st);
            NFA_State nfa_start=nfa.getStart_state();
            start.getEmpty_transitions().add(nfa_start);
            combined.add_accepting_state(nfa.getAccepting_states().iterator().next());
            combined.getStates().addAll(nfa.getStates());
            NFA_State n=nfa.getAccepting_states().iterator().next();
            acceptence.put(n,st);
        }
        combined.getStates().add(start);
        return combined;
    }

    private NFA deepClone(NFA nfa)
    {
        HashMap<NFA_State,NFA_State>created=new HashMap<>();
        NFA cloned=new NFA(dfs(nfa.getStart_state(),created));

        cloned.getStates().addAll(created.values());
        for (NFA_State nfa_state:nfa.getAccepting_states())
        {
            cloned.add_accepting_state(created.get(nfa_state));
        }
        return cloned;
    }

    private NFA_State dfs(NFA_State start_state, HashMap<NFA_State,NFA_State> created)
    {
        if (start_state==null)return null;


        if(created.containsKey(start_state)) {
            return created.get(start_state);
        }
        NFA_State currentNfa = new NFA_State();
        created.put(start_state, currentNfa);
        for(Character c:start_state.getTransitions().keySet()) {
            for (NFA_State n:start_state.getTransitions().get(c)){
                currentNfa.insert_transition(c,dfs(n,created));}
        }
        for (NFA_State n:start_state.getEmpty_transitions()){
            currentNfa.getEmpty_transitions().add(dfs(n,created));}
        return currentNfa;
    }
}