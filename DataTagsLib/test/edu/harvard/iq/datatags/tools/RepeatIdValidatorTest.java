package edu.harvard.iq.datatags.tools;

import edu.harvard.iq.datatags.model.graphs.nodes.EndNode;
import edu.harvard.iq.datatags.model.types.CompoundSlot;
import edu.harvard.iq.datatags.parser.decisiongraph.CompilationUnit;
import edu.harvard.iq.datatags.parser.decisiongraph.ast.AstNode;
import edu.harvard.iq.datatags.parser.exceptions.DataTagsParseException;
import edu.harvard.iq.datatags.tools.ValidationMessage.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Naomi
 */
public class RepeatIdValidatorTest {
    
    RepeatIdValidator instance;
    
    public RepeatIdValidatorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        instance = new RepeatIdValidator();
    }
    
    @After
    public void tearDown() {
    }

    
    @Test
    public void validateRepeatIdsTest_noId() throws DataTagsParseException {
        String code = "[call: ppraCompliance]\n" +
                      "[call: ferpaCompliance ]\n" +
                      "[call: govRecsCompliance ]";
        
        CompilationUnit cu = new CompilationUnit(code);
        cu.compile(new CompoundSlot("", ""), new EndNode("[SYN-END]"), new ArrayList<>());
        List<? extends AstNode> refs = cu.getParsedFile().getAstNodes();
        List<ValidationMessage> messages = instance.validate(refs);
        assertEquals(Collections.emptyList(), messages);
    }
    
    @Test
    public void validateRepeatIdsTest_diffIds() throws DataTagsParseException {
        String code = "[>personalData< call: medicalRecordsCompliance ]" +
                      "[>medicalRecordsCompliance< call: ppraCompliance]" +
                      "[>MR2< call: ppraCompliance]";
        CompilationUnit cu = new CompilationUnit(code);
        cu.compile(new CompoundSlot("", ""), new EndNode("[SYN-END]"), new ArrayList<>());
        List<? extends AstNode> refs = cu.getParsedFile().getAstNodes();
        List<ValidationMessage> messages = instance.validate(refs);
        assertEquals(Collections.emptyList(), messages);
    }
    
    @Test
    public void validateRepeatIdsTest_sameIds() throws DataTagsParseException {
        String code = "[>personalData< call: medicalRecordsCompliance ]" +
                      "[>medicalRecordsCompliance< call: ppraCompliance]" +
                      "[>personalData< call: ppraCompliance]";
        CompilationUnit cu = new CompilationUnit(code);
        cu.compile(new CompoundSlot("", ""), new EndNode("[SYN-END]"), new ArrayList<>());
        List<? extends AstNode> refs = cu.getParsedFile().getAstNodes();
        List<ValidationMessage> messages = instance.validate(refs);
        List<ValidationMessage> expected = Collections.singletonList(new ValidationMessage(Level.ERROR, "Duplicate node id: \"personalData\"."));
        assertEquals(expected, messages);
    }
    
    // THIS NEEDS TO FAIL: FIX THE REPEATIDVALIDATOR WITH VISITORS ASAP
    @Test
    public void validateRepeatIdsTest_layeredIds() throws DataTagsParseException {
        String code = 
                  "[>personalData< ask: {text: first}"
                + "{answers:"
                + "   {yes: [>repeat< ask: "
                + "       {text: second}"
                + "       {answers: {no: [>todo1< todo: nothing!]}}]}}]"
                + "[>repeat< ask: "
                + "    {text: is this a repeat?}"
                + "    {answers: {yes: [>todo1< todo: yes.]}}]";
        
        CompilationUnit cu = new CompilationUnit(code);
        cu.compile(new CompoundSlot("", ""), new EndNode("[SYN-END]"), new ArrayList<>());
        List<? extends AstNode> refs = cu.getParsedFile().getAstNodes();
        
        List<ValidationMessage> messages = instance.validate(refs);
        assertEquals( Arrays.asList(new ValidationMessage(Level.ERROR, "Duplicate node id: \"repeat\"."),
                               new ValidationMessage(Level.ERROR, "Duplicate node id: \"todo1\".")),
                      messages);
    }

    
}
