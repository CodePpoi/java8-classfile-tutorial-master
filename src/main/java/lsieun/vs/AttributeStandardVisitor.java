package lsieun.vs;

import lsieun.classfile.Attributes;
import lsieun.classfile.ConstantPool;
import lsieun.classfile.attrs.*;
import lsieun.classfile.attrs.annotation.*;
import lsieun.classfile.attrs.annotation.type.*;
import lsieun.cst.AccessConst;
import lsieun.cst.CPConst;
import lsieun.cst.StackMapConst;
import lsieun.utils.ByteDashboard;
import lsieun.utils.HexFormat;
import lsieun.utils.HexUtils;

import java.nio.charset.StandardCharsets;
import java.util.Formatter;

public class AttributeStandardVisitor extends DefaultVisitor {
    public static final String INDENT_00_SPACE = "";
    public static final String INDENT_04_SPACE = "    ";
    public static final String INDENT_08_SPACE = "        ";
    public static final String INDENT_12_SPACE = "            ";
    public static final String INDENT_16_SPACE = "                ";

    private ConstantPool constant_pool;
    private String prefix;

    public AttributeStandardVisitor(ConstantPool constant_pool) {
        this.constant_pool = constant_pool;
        this.prefix = INDENT_00_SPACE;
    }

    private void left_shift() {
        int length = prefix.length();
        if (length > 16) {
            prefix = INDENT_16_SPACE;
        } else if (length > 12) {
            prefix = INDENT_12_SPACE;
        } else if (length > 8) {
            prefix = INDENT_08_SPACE;
        } else if (length > 4) {
            prefix = INDENT_04_SPACE;
        } else {
            prefix = INDENT_00_SPACE;
        }
    }

    private void right_shift() {
        int length = prefix.length();
        if (length < 4) {
            prefix = INDENT_04_SPACE;
        } else if (length < 8) {
            prefix = INDENT_08_SPACE;
        } else if (length < 12) {
            prefix = INDENT_12_SPACE;
        } else {
            prefix = INDENT_16_SPACE;
        }
    }

    @Override
    public void visitAttributes(Attributes obj) {
        int count = obj.attributes_count;
        AttributeInfo[] entries = obj.entries;

        String countLine = String.format("attributes_count='%s' (%d)", obj.hex(), count);
        System.out.println(countLine);

        System.out.println("attributes");
        for (int i = 0; i < count; i++) {
            AttributeInfo item = entries[i];
            String hexCode = item.hex();
            String attrName = constant_pool.getConstantString(item.attribute_name_index, CPConst.CONSTANT_Utf8);
            System.out.println(String.format("--->|%03d| %s:", i, attrName));
            System.out.println("HexCode: " + hexCode);
            item.accept(this);
        }
    }

    @Override
    public void visitAttributeInfo(AttributeInfo obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);
    }

    @Override
    public void visitAnnotationDefault(AnnotationDefault obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        ElementValue element_value = obj.default_value;

        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        display_element_value_pair(element_value, bd, fm);
        System.out.println(sb.toString());
    }

    @Override
    public void visitBootstrapMethods(BootstrapMethods obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        String format = "%s='%s' (%s)%n";
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);

        int num_bootstrap_methods = obj.num_bootstrap_methods;
        fm.format(prefix + format, "num_bootstrap_methods", HexUtils.toHex(bd.nextN(2)), num_bootstrap_methods);

        if (num_bootstrap_methods > 0) {
            for (int i = 0; i < obj.num_bootstrap_methods; i++) {
                BootstrapMethod entry = obj.entries[i];
                int bootstrap_method_ref = entry.bootstrap_method_ref;
                int num_bootstrap_arguments = entry.num_bootstrap_arguments;
                int[] bootstrap_arguments = entry.bootstrap_arguments;
                String bootstrap_arguments_str = array2str(bootstrap_arguments);
                fm.format(prefix + "bootstrap_methods[%d] {%n", i);
                {
                    right_shift();
                    fm.format(prefix + format, "bootstrap_method_ref", HexUtils.toHex(bd.nextN(2)), "#" + bootstrap_method_ref);
                    fm.format(prefix + format, "num_bootstrap_arguments", HexUtils.toHex(bd.nextN(2)), num_bootstrap_arguments);
                    fm.format(prefix + format, "bootstrap_arguments", HexUtils.toHex(bd.nextN(2 * num_bootstrap_arguments)), bootstrap_arguments_str);
                    left_shift();
                }
                fm.format(prefix + "}%n");
            }
        }
        System.out.print(sb.toString());
    }

    public static String array2str(int[] array) {
        int length = array.length;
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        fm.format("[");
        for (int i = 0; i < length - 1; i++) {
            int item = array[i];
            fm.format("#%s,", item);
        }
        if (length > 0) {
            fm.format("#%s", array[length - 1]);
        }
        fm.format("]");
        return sb.toString();
    }

    @Override
    public void visitCode(Code obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);


        String format = "%s='%s' (%s)%n";
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        fm.format(format, "max_stack", HexUtils.toHex(bd.nextN(2)), obj.max_stack);
        fm.format(format, "max_locals", HexUtils.toHex(bd.nextN(2)), obj.max_locals);
        fm.format(format, "code_length", HexUtils.toHex(bd.nextN(4)), obj.code_length);
        byte[] code_bytes = bd.nextN(obj.code_length);
        fm.format("code: %s%n", HexUtils.format(code_bytes, HexFormat.FORMAT_FF_FF_32));
        fm.format(format, "exception_table_length", HexUtils.toHex(bd.nextN(2)), obj.exception_table_length);
        for (int i = 0; i < obj.exception_table_length; i++) {
            ExceptionTable exception_table = obj.exception_table_array[i];
            byte[] start_pc_bytes = bd.nextN(2);
            byte[] end_pc_bytes = bd.nextN(2);
            byte[] handler_pc_bytes = bd.nextN(2);
            byte[] catch_type_bytes = bd.nextN(2);

            fm.format(prefix + "exception_table[%d] {%n", i);
            {
                right_shift();
                fm.format(prefix + format, "start_pc", HexUtils.toHex(start_pc_bytes), exception_table.start_pc);
                fm.format(prefix + format, "end_pc", HexUtils.toHex(end_pc_bytes), exception_table.end_pc);
                fm.format(prefix + format, "handler_pc", HexUtils.toHex(handler_pc_bytes), exception_table.handler_pc);
                fm.format(prefix + format, "catch_type", HexUtils.toHex(catch_type_bytes), "#" + exception_table.catch_type);
                left_shift();
            }
            fm.format(prefix + "}%n");
        }
        fm.format(format, "attributes_count", HexUtils.toHex(bd.nextN(2)), obj.attributes.attributes_count);
        for (int i = 0; i < obj.attributes.attributes_count; i++) {
            AttributeInfo entry = obj.attributes.entries[i];
            right_shift();
            String attrName = constant_pool.getConstantString(entry.attribute_name_index, CPConst.CONSTANT_Utf8);
            fm.format(prefix + "%s: %s%n", attrName, HexUtils.toHex(entry.bytes));
            left_shift();
        }
        System.out.print(sb.toString());
    }

    @Override
    public void visitConstantValue(ConstantValue obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        String format = "%s='%s' (%s)";
        String line = String.format(format, "constantvalue_index", HexUtils.toHex(bd.nextN(2)), "#" + obj.constantvalue_index);
        System.out.println(line);
    }

    @Override
    public void visitEnclosingMethod(EnclosingMethod obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        String format = "%s='%s' (%s)%n";
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        fm.format(prefix + format, "class_index", HexUtils.toHex(bd.nextN(2)), "#" + obj.class_index);
        String method_name = "";
        if (obj.method_index != 0) {
            method_name = constant_pool.getConstant(obj.method_index).value;
        }
        fm.format(format, "method_index", HexUtils.toHex(bd.nextN(2)), "#" + obj.method_index);
        System.out.print(sb.toString());
    }

    @Override
    public void visitExceptions(Exceptions obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        int number_of_exceptions = obj.number_of_exceptions;
        String format = "%s='%s' (%s)%n";
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        fm.format(prefix + format, "number_of_exceptions", HexUtils.toHex(bd.nextN(2)), number_of_exceptions);
        if (number_of_exceptions > 0) {
            fm.format(prefix + "exception_index_table {%n");
            right_shift();
            for (int i = 0; i < number_of_exceptions; i++) {
                fm.format(prefix + format, "exception_index", HexUtils.toHex(bd.nextN(2)), "#" + obj.exception_index_array[i]);
            }
            left_shift();
            fm.format(prefix + "}%n");
        }

        System.out.println(sb.toString());
    }

    @Override
    public void visitInnerClasses(InnerClasses obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        String format = "%s='%s' (%s)%n";
        int number_of_classes = obj.number_of_classes;
        fm.format(prefix + format, "number_of_classes", HexUtils.toHex(bd.nextN(2)), number_of_classes);

        if (number_of_classes > 0) {
            for (int i = 0; i < obj.number_of_classes; i++) {
                InnerClass entry = obj.entries[i];
                fm.format(prefix + "classes[%d] {%n", i);
                {
                    right_shift();
                    fm.format(prefix + format, "inner_class_info_index", HexUtils.toHex(bd.nextN(2)), "#" + entry.inner_class_info_index);
                    fm.format(prefix + format, "outer_class_info_index", HexUtils.toHex(bd.nextN(2)), "#" + entry.outer_class_info_index);
                    fm.format(prefix + format, "inner_name_index", HexUtils.toHex(bd.nextN(2)), "#" + entry.inner_name_index);
                    fm.format(prefix + format, "inner_class_access_flags", HexUtils.toHex(bd.nextN(2)), AccessConst.getInnerClassAccessFlagsString(entry.inner_class_access_flags));
                    left_shift();
                }
                fm.format(prefix + "}%n");
            }
        }

        System.out.print(sb.toString());
    }

    @Override
    public void visitLineNumberTable(LineNumberTable obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        String format = "%s='%s' (%s)%n";
        int line_number_table_length = obj.line_number_table_length;
        fm.format(prefix + format, "line_number_table_length", HexUtils.toHex(bd.nextN(2)), line_number_table_length);

        if (line_number_table_length > 0) {
            for (int i = 0; i < line_number_table_length; i++) {
                LineNumber entry = obj.line_number_table[i];
                fm.format(prefix + "line_number_table[%d] {%n", i);
                {
                    right_shift();
                    fm.format(prefix + format, "start_pc", HexUtils.toHex(bd.nextN(2)), entry.start_pc);
                    fm.format(prefix + format, "line_number", HexUtils.toHex(bd.nextN(2)), entry.line_number);
                    left_shift();
                }
                fm.format(prefix + "}%n");
            }
        }

        System.out.print(sb.toString());
    }

    @Override
    public void visitLocalVariableTable(LocalVariableTable obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        String format = "%s='%s' (%s)%n";
        int local_variable_table_length = obj.local_variable_table_length;
        fm.format(prefix + format, "local_variable_table_length", HexUtils.toHex(bd.nextN(2)), local_variable_table_length);

        if (local_variable_table_length > 0) {
            for (int i = 0; i < local_variable_table_length; i++) {
                LocalVariable entry = obj.entries[i];
                fm.format(prefix + "local_variable_table[%d] {%n", i);
                {
                    right_shift();
                    fm.format(prefix + format, "start_pc", HexUtils.toHex(bd.nextN(2)), entry.start_pc);
                    fm.format(prefix + format, "length", HexUtils.toHex(bd.nextN(2)), entry.length);
                    fm.format(prefix + format, "name_index", HexUtils.toHex(bd.nextN(2)), "#" + entry.name_index);
                    fm.format(prefix + format, "descriptor_index", HexUtils.toHex(bd.nextN(2)), "#" + entry.descriptor_index);
                    fm.format(prefix + format, "index", HexUtils.toHex(bd.nextN(2)), entry.index);
                    left_shift();
                }
                fm.format(prefix + "}%n");
            }
        }

        System.out.print(sb.toString());
    }

    @Override
    public void visitLocalVariableTypeTable(LocalVariableTypeTable obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        String format = "%s='%s' (%s)%n";
        int local_variable_type_table_length = obj.local_variable_type_table_length;
        fm.format(prefix + format, "local_variable_type_table_length", HexUtils.toHex(bd.nextN(2)), local_variable_type_table_length);

        if (local_variable_type_table_length > 0) {
            for (int i = 0; i < local_variable_type_table_length; i++) {
                LocalVariableType entry = obj.entries[i];
                fm.format(prefix + "local_variable_type_table[%d] {%n", i);
                {
                    right_shift();
                    fm.format(prefix + format, "start_pc", HexUtils.toHex(bd.nextN(2)), entry.start_pc);
                    fm.format(prefix + format, "length", HexUtils.toHex(bd.nextN(2)), entry.length);
                    fm.format(prefix + format, "name_index", HexUtils.toHex(bd.nextN(2)), "#" + entry.name_index);
                    fm.format(prefix + format, "signature_index", HexUtils.toHex(bd.nextN(2)), "#" + entry.signature_index);
                    fm.format(prefix + format, "index", HexUtils.toHex(bd.nextN(2)), entry.index);
                    left_shift();
                }
                fm.format(prefix + "}%n");
            }
        }

        System.out.print(sb.toString());
    }

    @Override
    public void visitRuntimeInvisibleAnnotations(RuntimeInvisibleAnnotations obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        int num_annotations = obj.num_annotations;
        AnnotationEntry[] annotations = obj.annotations;
        displayRuntimeAnnotations(num_annotations, annotations, bd);
    }

    @Override
    public void visitRuntimeVisibleAnnotations(RuntimeVisibleAnnotations obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        int num_annotations = obj.num_annotations;
        AnnotationEntry[] annotations = obj.annotations;
        displayRuntimeAnnotations(num_annotations, annotations, bd);
    }

    private void displayRuntimeAnnotations(int num_annotations, AnnotationEntry[] annotations, ByteDashboard bd) {
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);

        String format = "%s='%s' (%s)%n";
        fm.format(prefix + format, "num_annotations", HexUtils.toHex(bd.nextN(2)), num_annotations);
        display_annotations(annotations, bd, fm);
        System.out.println(sb.toString());
    }

    private void display_annotations(AnnotationEntry[] annotations, ByteDashboard bd, Formatter fm) {
        int length = annotations.length;
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                AnnotationEntry annotation = annotations[i];
                fm.format(prefix + "annotation[%d] {%n", i);
                {
                    right_shift();
                    display_one_annotation(annotation, bd, fm);
                    left_shift();
                }
                fm.format(prefix + "}%n");
            }
        }
    }

    private void display_one_annotation(AnnotationEntry annotationEntry, ByteDashboard bd, Formatter fm) {
        String format = "%s='%s' (%s)%n";
        fm.format(prefix + format, "type_index", HexUtils.toHex(bd.nextN(2)), "#" + annotationEntry.type_index);
        fm.format(prefix + format, "num_element_value_pairs", HexUtils.toHex(bd.nextN(2)), annotationEntry.num_element_value_pairs);

        ElementValuePair[] element_value_pairs = annotationEntry.element_value_pair_list;
        if (annotationEntry.num_element_value_pairs > 0) {
            display_element_value_pairs(element_value_pairs, bd, fm);
        }
    }

    @Override
    public void visitRuntimeInvisibleParameterAnnotations(RuntimeInvisibleParameterAnnotations obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        int num_parameters = obj.num_parameters;
        ParameterAnnotation[] parameter_annotations = obj.parameter_annotations;
        displayRuntimeParameterAnnotations(num_parameters, parameter_annotations, bd);
    }

    @Override
    public void visitRuntimeVisibleParameterAnnotations(RuntimeVisibleParameterAnnotations obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        int num_parameters = obj.num_parameters;
        ParameterAnnotation[] parameter_annotations = obj.parameter_annotations;
        displayRuntimeParameterAnnotations(num_parameters, parameter_annotations, bd);
    }

    private void displayRuntimeParameterAnnotations(int num_parameters, ParameterAnnotation[] parameter_annotations, ByteDashboard bd) {
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);

        String format = "%s='%s' (%s)%n";
        fm.format(prefix + format, "num_parameters", HexUtils.toHex(bd.nextN(1)), num_parameters);
        if (num_parameters > 0) {
            for (int i = 0; i < num_parameters; i++) {
                ParameterAnnotation parameter_annotation = parameter_annotations[i];
                fm.format(prefix + "parameter_annotations[%d] {%n", i);
                {
                    right_shift();
                    int num_annotations = parameter_annotation.num_annotations;
                    AnnotationEntry[] annotations = parameter_annotation.annotations;

                    fm.format(prefix + format, "num_annotations", HexUtils.toHex(bd.nextN(2)), num_annotations);
                    display_annotations(annotations, bd, fm);
                    left_shift();
                }
                fm.format(prefix + "}%n");
            }
        }
        System.out.println(sb.toString());
    }

    @Override
    public void visitRuntimeInvisibleTypeAnnotations(RuntimeInvisibleTypeAnnotations obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        int num_annotations = obj.num_annotations;
        TypeAnnotation[] annotations = obj.annotations;
        displayRuntimeTypeAnnotations(num_annotations, annotations, bd);
    }

    @Override
    public void visitRuntimeVisibleTypeAnnotations(RuntimeVisibleTypeAnnotations obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        int num_annotations = obj.num_annotations;
        TypeAnnotation[] annotations = obj.annotations;
        displayRuntimeTypeAnnotations(num_annotations, annotations, bd);
    }

    public void displayRuntimeTypeAnnotations(int num_annotations, TypeAnnotation[] annotations, ByteDashboard bd) {
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);

        String format = "%s='%s' (%s)%n";
        fm.format(format, "num_annotations", HexUtils.toHex(bd.nextN(2)), num_annotations);
        if (num_annotations > 0) {
            displayTypeAnnotations(annotations, bd, fm);
        }

        System.out.println(sb.toString());
    }

    public void displayTypeAnnotations(TypeAnnotation[] annotations, ByteDashboard bd, Formatter fm) {
        int length = annotations.length;
        String format = "%s='%s' (%s)%n";

        for (int i = 0; i < length; i++) {
            TypeAnnotation annotation = annotations[i];
            int target_type = annotation.target_type;
            fm.format(prefix + "annotations[%d] {%n", i);
            right_shift();

            fm.format(prefix + format, "target_type", HexUtils.toHex(bd.nextN(1)), target_type);

            fm.format(prefix + "target_info {%n");
            {
                right_shift();
                switch (target_type) {
                    case 0x00:
                    case 0x01:
                        TypeParameterTarget type_parameter_target = annotation.target_info.type_parameter_target;
                        fm.format(prefix + "type_parameter_target {%n");
                        right_shift();
                        fm.format(prefix + format, "type_parameter_index", HexUtils.toHex(bd.nextN(1)), "#" + type_parameter_target.type_parameter_index);
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    case 0x10:
                        SuperTypeTarget supertype_target = annotation.target_info.supertype_target;
                        fm.format(prefix + "supertype_target {%n");
                        right_shift();
                        fm.format(prefix + format, "supertype_index", HexUtils.toHex(bd.nextN(2)), "#" + supertype_target.supertype_index);
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    case 0x11:
                    case 0x12:
                        TypeParameterBoundTarget type_parameter_bound_target = annotation.target_info.type_parameter_bound_target;
                        fm.format(prefix + "type_parameter_bound_target {%n");
                        right_shift();
                        fm.format(prefix + format, "type_parameter_index", HexUtils.toHex(bd.nextN(1)), "#" + type_parameter_bound_target.type_parameter_index);
                        fm.format(prefix + format, "bound_index", HexUtils.toHex(bd.nextN(1)), "#" + type_parameter_bound_target.bound_index);
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    case 0x13:
                    case 0x14:
                    case 0x15:
                        EmptyTarget empty_target = annotation.target_info.empty_target;
                        fm.format(prefix + "empty_target {%n");
                        fm.format(prefix + "}%n");
                        break;
                    case 0x16:
                        FormalParameterTarget method_formal_parameter_target = annotation.target_info.method_formal_parameter_target;
                        fm.format(prefix + "formal_parameter_target {%n");
                        right_shift();
                        fm.format(prefix + format, "formal_parameter_index", HexUtils.toHex(bd.nextN(1)), "#" + method_formal_parameter_target.formal_parameter_index);
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    case 0x17:
                        ThrowsTarget throws_target = annotation.target_info.throws_target;
                        fm.format(prefix + "throws_target {%n");
                        right_shift();
                        fm.format(prefix + format, "throws_type_index", HexUtils.toHex(bd.nextN(2)), "#" + throws_target.throws_type_index);
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    case 0x40:
                    case 0x41:
                        LocalVarTarget localvar_target = annotation.target_info.localvar_target;
                        fm.format(prefix + "localvar_target {%n");
                        right_shift();
                        int table_length = localvar_target.table_length;
                        fm.format(prefix + format, "table_length", HexUtils.toHex(bd.nextN(2)), table_length);
                        if (table_length > 0) {
                            for (int j = 0; j < table_length; j++) {
                                Table table = localvar_target.tables[i];
                                fm.format(prefix + "table[%d] {%n", j);
                                right_shift();
                                fm.format(prefix + format, "start_pc", HexUtils.toHex(bd.nextN(2)), table.start_pc);
                                fm.format(prefix + format, "length", HexUtils.toHex(bd.nextN(2)), table.length);
                                fm.format(prefix + format, "index", HexUtils.toHex(bd.nextN(2)), table.index);
                                left_shift();
                                fm.format(prefix + "}%n");
                            }
                        }
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    case 0x42:
                        CatchTarget catch_target = annotation.target_info.catch_target;
                        fm.format(prefix + "catch_target {%n");
                        right_shift();
                        fm.format(prefix + format, "exception_table_index", HexUtils.toHex(bd.nextN(2)), "#" + catch_target.exception_table_index);
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    case 0x43:
                    case 0x44:
                    case 0x45:
                    case 0x46:
                        OffsetTarget offset_target = annotation.target_info.offset_target;
                        fm.format(prefix + "offset_target {%n");
                        right_shift();
                        fm.format(prefix + format, "offset", HexUtils.toHex(bd.nextN(2)), offset_target.offset);
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    case 0x47:
                        TypeArgumentTarget type_argument_target = annotation.target_info.type_argument_target;
                        fm.format(prefix + "type_argument_target {%n");
                        right_shift();
                        fm.format(prefix + format, "offset", HexUtils.toHex(bd.nextN(2)), type_argument_target.offset);
                        fm.format(prefix + format, "type_argument_index", HexUtils.toHex(bd.nextN(1)), type_argument_target.type_argument_index);
                        left_shift();
                        fm.format(prefix + "}%n");
                        break;
                    default:
                        throw new RuntimeException("Unknown target_type: " + target_type);
                }
                left_shift();
            }
            fm.format(prefix + "}%n");

            fm.format(prefix + "target_path {%n");
            {
                right_shift();
                int path_length = annotation.target_path.path_length;
                fm.format(prefix + format, "path_length", HexUtils.toHex(bd.nextN(1)), path_length);
                if (path_length > 0) {
                    for (int j = 0; j < path_length; j++) {
                        Path path = annotation.target_path.pathes[j];
                        fm.format(prefix + "path[%d] {%n", j);
                        right_shift();
                        fm.format(prefix + format, "type_path_kind", HexUtils.toHex(bd.nextN(1)), path.type_path_kind);
                        fm.format(prefix + format, "type_argument_index", HexUtils.toHex(bd.nextN(1)), path.type_argument_index);
                        left_shift();
                        fm.format(prefix + "}%n");
                    }
                }
                left_shift();
            }
            fm.format(prefix + "}%n");

            fm.format(prefix + format, "type_index", HexUtils.toHex(bd.nextN(2)), "#" + annotation.type_index);

            int num_element_value_pairs = annotation.num_element_value_pairs;
            fm.format(prefix + format, "num_element_value_pairs", HexUtils.toHex(bd.nextN(2)), num_element_value_pairs);

            if (num_element_value_pairs > 0) {
                display_element_value_pairs(annotation.element_value_pairs, bd, fm);
            }

            left_shift();
            fm.format(prefix + "}%n");

        }

    }

    private void display_element_value_pairs(ElementValuePair[] element_value_pairs, ByteDashboard bd, Formatter fm) {
        int length = element_value_pairs.length;
        if (length > 0) {
            String format = "%s='%s' (%s)%n";

            for (int i = 0; i < length; i++) {
                ElementValuePair element_value_pair = element_value_pairs[i];
                int element_name_index = element_value_pair.element_name_index;
                ElementValue element_value = element_value_pair.value;

                fm.format(prefix + "element_value_pairs[%d] {%n", i);
                right_shift();
                fm.format(prefix + format, "element_name_index", HexUtils.toHex(bd.nextN(2)), "#" + element_name_index);
                display_element_value_pair(element_value, bd, fm);
                left_shift();
                fm.format(prefix + "}%n");
            }
        }
    }

    private void display_element_value_pair(ElementValue element_value, ByteDashboard bd, Formatter fm) {
        String format = "%s='%s' (%s)%n";
        fm.format(prefix + "element_value {%n");
        right_shift();

        fm.format(prefix + format, "tag", HexUtils.toHex(bd.nextN(1)), (char) element_value.type);
        if (element_value instanceof SimpleElementValue) {
            SimpleElementValue simpleElementValue = (SimpleElementValue) element_value;
            String value = "#" + simpleElementValue.const_value_index + ": " + simpleElementValue.stringifyValue();
            fm.format(prefix + format, "const_value_index", HexUtils.toHex(bd.nextN(2)), value);
        } else if (element_value instanceof EnumElementValue) {
            EnumElementValue enumElementValue = (EnumElementValue) element_value;
            fm.format(prefix + format, "type_name_index", HexUtils.toHex(bd.nextN(2)), "#" + enumElementValue.type_name_index);
            fm.format(prefix + format, "const_name_index", HexUtils.toHex(bd.nextN(2)), "#" + enumElementValue.const_name_index);
        } else if (element_value instanceof ClassElementValue) {
            ClassElementValue classElementValue = (ClassElementValue) element_value;
            fm.format(prefix + format, "class_info_index", HexUtils.toHex(bd.nextN(2)), "#" + classElementValue.class_info_index);
        } else if (element_value instanceof AnnotationElementValue) {
            AnnotationElementValue annotation_element_value = (AnnotationElementValue) element_value;
            fm.format(prefix + format, "type_index", HexUtils.toHex(bd.nextN(2)), "#" + annotation_element_value.annotation_entry.type_index);
            fm.format(prefix + format, "num_element_value_pairs", HexUtils.toHex(bd.nextN(2)), annotation_element_value.annotation_entry.num_element_value_pairs);
            display_element_value_pairs(annotation_element_value.annotation_entry.element_value_pair_list, bd, fm);
        } else if (element_value instanceof ArrayElementValue) {
            ArrayElementValue arrayElementValue = (ArrayElementValue) element_value;
            fm.format(prefix + format, "num_values", HexUtils.toHex(bd.nextN(2)), arrayElementValue.num_values);
            for (int i = 0; i < arrayElementValue.num_values; i++) {
                display_element_value_pair(arrayElementValue.entries[i], bd, fm);
            }
        }

        left_shift();
        fm.format(prefix + "}%n");
    }

    @Override
    public void visitSignature(Signature obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        String format = "%s='%s' (%s)";
        System.out.println(String.format(format, "signature_index", HexUtils.toHex(bd.nextN(2)), "#" + obj.signature_index));

    }

    @Override
    public void visitSourceDebugExtension(SourceDebugExtension obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        String format = "%s=%n%s";
        System.out.println(String.format(format, "debug_extension", HexUtils.format(obj.debug_extension, HexFormat.FORMAT_FF_SPACE_FF_32)));
    }

    @Override
    public void visitSourceFile(SourceFile obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        String value = "#" + obj.sourcefile_index;
        String format = "%s='%s' (%s)";
        String line = String.format(format, "sourcefile_index", HexUtils.toHex(bd.nextN(2)), value);
        System.out.println(line);
    }

    @Override
    public void visitStackMapTable(StackMapTable obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        String format = "%s='%s' (%s)%n";
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);

        int number_of_entries = obj.number_of_entries;
        fm.format(prefix + format, "number_of_entries", HexUtils.toHex(bd.nextN(2)), number_of_entries);
        if (number_of_entries > 0) {
            int code_index = -1;
            for (int i = 0; i < number_of_entries; i++) {
                StackMapFrame entry = obj.entries[i];
                int frame_type = entry.frame_type;
                int byte_code_offset = entry.byte_code_offset;
                code_index = code_index + byte_code_offset + 1;
                StackMapType[] types_of_locals = entry.types_of_locals;
                StackMapType[] types_of_stack_items = entry.types_of_stack_items;
                fm.format(prefix + "stack_map_frame[%d]@%d {%n", i, code_index);
                {
                    right_shift();
                    if (frame_type >= StackMapConst.SAME_FRAME && frame_type <= StackMapConst.SAME_FRAME_MAX) {
                        fm.format(prefix + "same_frame {%n");
                        {
                            right_shift();
                            fm.format(prefix + format, "frame_type", HexUtils.toHex(bd.nextN(1)), frame_type);
                            left_shift();
                        }
                        fm.format(prefix + "}%n");
                    } else if (frame_type >= StackMapConst.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                            frame_type <= StackMapConst.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
                        fm.format(prefix + "same_locals_1_stack_item_frame {%n");
                        {
                            right_shift();
                            fm.format(prefix + format, "frame_type", HexUtils.toHex(bd.nextN(1)), frame_type);
                            display_verification_type_info("stack", types_of_stack_items, bd, fm);
                            left_shift();
                        }
                        fm.format(prefix + "}%n");
                    } else if (frame_type == StackMapConst.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
                        fm.format(prefix + "same_locals_1_stack_item_frame_extended {%n");
                        {
                            right_shift();
                            fm.format(prefix + format, "frame_type", HexUtils.toHex(bd.nextN(1)), frame_type);
                            fm.format(prefix + format, "offset_delta", HexUtils.toHex(bd.nextN(2)), byte_code_offset);
                            display_verification_type_info("stack", types_of_stack_items, bd, fm);
                            left_shift();
                        }
                        fm.format(prefix + "}%n");
                    } else if (frame_type >= StackMapConst.CHOP_FRAME && frame_type <= StackMapConst.CHOP_FRAME_MAX) {
                        fm.format(prefix + "chop_frame {%n");
                        {
                            right_shift();
                            fm.format(prefix + format, "frame_type", HexUtils.toHex(bd.nextN(1)), frame_type);
                            fm.format(prefix + format, "offset_delta", HexUtils.toHex(bd.nextN(2)), byte_code_offset);
                            left_shift();
                        }
                        fm.format(prefix + "}%n");
                    } else if (frame_type == StackMapConst.SAME_FRAME_EXTENDED) {
                        fm.format(prefix + "same_frame_extended {%n");
                        {
                            right_shift();
                            fm.format(prefix + format, "frame_type", HexUtils.toHex(bd.nextN(1)), frame_type);
                            fm.format(prefix + format, "offset_delta", HexUtils.toHex(bd.nextN(2)), byte_code_offset);
                            left_shift();
                        }
                        fm.format(prefix + "}%n");
                    } else if (frame_type >= StackMapConst.APPEND_FRAME && frame_type <= StackMapConst.APPEND_FRAME_MAX) {
                        fm.format(prefix + "append_frame {%n");
                        {
                            right_shift();
                            fm.format(prefix + format, "frame_type", HexUtils.toHex(bd.nextN(1)), frame_type);
                            fm.format(prefix + format, "offset_delta", HexUtils.toHex(bd.nextN(2)), byte_code_offset);
                            display_verification_type_info("local", types_of_locals, bd, fm);
                            left_shift();
                        }
                        fm.format(prefix + "}%n");
                    } else if (frame_type == StackMapConst.FULL_FRAME) {
                        fm.format(prefix + "full_frame {%n");
                        {
                            right_shift();
                            fm.format(prefix + format, "frame_type", HexUtils.toHex(bd.nextN(1)), frame_type);
                            fm.format(prefix + format, "offset_delta", HexUtils.toHex(bd.nextN(2)), byte_code_offset);
                            fm.format(prefix + format, "number_of_locals", HexUtils.toHex(bd.nextN(2)), types_of_locals.length);
                            display_verification_type_info("local", types_of_locals, bd, fm);
                            fm.format(prefix + format, "number_of_stack_items", HexUtils.toHex(bd.nextN(2)), types_of_stack_items.length);
                            display_verification_type_info("stack", types_of_stack_items, bd, fm);
                            left_shift();
                        }
                        fm.format(prefix + "}%n");
                    } else {
                        /* Can't happen */
                        throw new RuntimeException("Invalid frame type found: " + frame_type);
                    }
                    left_shift();
                }
                fm.format(prefix + "}%n");
            }
        }

        System.out.print(sb.toString());
    }

    private void display_verification_type_info(String label, StackMapType[] types, ByteDashboard bd, Formatter fm) {
        if (types != null && types.length > 0) {
            int length = types.length;
            for (int i = 0; i < length; i++) {
                StackMapType entry = types[i];
                display_verification_type_info(label, i, entry, bd, fm);
            }
        }
    }

    private void display_verification_type_info(String label, int index, StackMapType stack_map_type, ByteDashboard bd, Formatter fm) {
        byte tag = stack_map_type.tag;
        String format = "%s='%s' (%s)%n";
        String rawName = StackMapConst.getItemRawName(tag);
        fm.format(prefix + "%s[%d] {%n", label, index);
        {
            right_shift();
            switch (tag) {
                case StackMapConst.ITEM_Bogus:
                case StackMapConst.ITEM_Integer:
                case StackMapConst.ITEM_Float:
                case StackMapConst.ITEM_Double:
                case StackMapConst.ITEM_Long:
                case StackMapConst.ITEM_Null:
                case StackMapConst.ITEM_InitObject: {
                    fm.format(prefix + "%s {%n", rawName);
                    {
                        right_shift();
                        fm.format(prefix + format, "tag", HexUtils.toHex(bd.nextN(1)), tag);
                        left_shift();
                    }
                    fm.format(prefix + "}%n");
                    break;
                }
                case StackMapConst.ITEM_Object: {
                    fm.format(prefix + "%s {%n", rawName);
                    {
                        right_shift();
                        fm.format(prefix + format, "tag", HexUtils.toHex(bd.nextN(1)), tag);
                        fm.format(prefix + format, "cpool_index", HexUtils.toHex(bd.nextN(2)), "#" + stack_map_type.index);
                        left_shift();
                    }
                    fm.format(prefix + "}%n");
                    break;
                }

                case StackMapConst.ITEM_NewObject: {
                    fm.format(prefix + "%s {%n", rawName);
                    {
                        right_shift();
                        fm.format(prefix + format, "tag", HexUtils.toHex(bd.nextN(1)), tag);
                        fm.format(prefix + format, "offset", HexUtils.toHex(bd.nextN(2)), stack_map_type.index);
                        left_shift();
                    }
                    fm.format(prefix + "}%n");
                    break;
                }
            }
            left_shift();
        }
        fm.format(prefix + "}%n");
    }

    @Override
    public void visitMethodParameters(MethodParameters obj) {
        byte[] bytes = obj.bytes;
        ByteDashboard bd = new ByteDashboard(bytes);
        visitAttributeCommon(obj, bd);

        int parameters_count = obj.parameters_count;
        MethodParameter[] parameters = obj.parameters;

        String format = "%s='%s' (%s)%n";
        StringBuilder sb = new StringBuilder();
        Formatter fm = new Formatter(sb);
        fm.format(prefix + format, "parameters_count", HexUtils.toHex(bd.nextN(1)), parameters_count);

        for (int i = 0; i < parameters_count; i++) {
            MethodParameter param = parameters[i];
            int name_index = param.name_index;
            int access_flags = param.access_flags;
            String access_flags_str;
            switch (access_flags) {
                case 0x000:
                    access_flags_str = "";
                    break;
                case 0x0010:
                    access_flags_str = "ACC_FINAL";
                    break;
                case 0x1000:
                    access_flags_str = "ACC_SYNTHETIC";
                    break;
                case 0x8000:
                    access_flags_str = "ACC_MANDATED";
                    break;
                default:
                    throw new RuntimeException("Unknown access flags: " + access_flags);
            }

            String name = constant_pool.getConstantString(name_index, CPConst.CONSTANT_Utf8);
            String name_value = "#" + name_index + ":" + name;
            fm.format(prefix + "parameters[%d] {%n", i);
            {
                right_shift();
                fm.format(prefix + format, "name_index", HexUtils.toHex(bd.nextN(2)), name_value);
                fm.format(prefix + format, "access_flags", HexUtils.toHex(bd.nextN(2)), access_flags_str);
                left_shift();
            }
            fm.format(prefix + "}%n");
        }
        System.out.println(sb.toString());
    }

    public void visitAttributeCommon(AttributeInfo obj, ByteDashboard bd) {
        int attribute_name_index = obj.attribute_name_index;
        int attribute_length = obj.attribute_length;

        String format = "%s='%s' (%s)";
        String attribute_name_line = String.format(format, "attribute_name_index", HexUtils.toHex(bd.nextN(2)), "#" + attribute_name_index);
        String attribute_length_line = String.format(format, "attribute_length", HexUtils.toHex(bd.nextN(4)), attribute_length);
        System.out.println(attribute_name_line);
        System.out.println(attribute_length_line);
    }
}
