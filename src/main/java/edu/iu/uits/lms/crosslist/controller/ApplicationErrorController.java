package edu.iu.uits.lms.crosslist.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;

@ControllerAdvice
@Slf4j
public class ApplicationErrorController {

    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Model model, Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);

        log.info("*** exception type is " + exception.getClass().getName());

        model.addAttribute("message", "An error has occured. See the stacktrace for details");
        model.addAttribute("error", stringWriter.toString());

        return "error";
    }
}
