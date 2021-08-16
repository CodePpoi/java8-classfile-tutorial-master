package lsieun.code.opcode;

import lsieun.cst.OpcodeConst;
import lsieun.code.Instruction;
import lsieun.code.visitors.OpcodeVisitor;
import lsieun.code.facet.ArithmeticInstruction;

public class DSUB extends Instruction implements ArithmeticInstruction {

    public DSUB() {
        super(OpcodeConst.DSUB, 1);
    }

    @Override
    public void accept(final OpcodeVisitor v) {
        v.visitDSUB(this);
    }

}
