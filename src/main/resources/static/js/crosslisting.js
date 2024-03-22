/*-
 * #%L
 * lms-lti-crosslist
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
var summaryNone = "<li id='summaryNone'>None</li>";
var summary = $('.summaryList').clone(true);
var checkedValue;

$(document).ready(function(){
    // this section sometimes processes before it actually exists
    // put it in this waitForElm method to make it wait until it exists in the dom
    waitForElm('#unavailable-sections-load').then((elm) => {
        loadUnavailableSections();
    });

    checkedValue = getCheckboxValues();

    checkboxEventRegistration();
    modalButtonToggle();

    // Canvas has a message listener to resize the iframe
    parent.postMessage(JSON.stringify({subject: 'lti.frameResize', height: $(document).height()}), '*');

    /* Reset Button */
    $('#reset-button').on('click', function(){

        //Reset section area
        $('.sectionsList li').each(function() {
            var li = $(this);
            var currCheckbox = li.find('input[type=checkbox]');
            var isChecked = currCheckbox.is(":checked");
            li.removeClass("currently_checked");
            if (li.hasClass('originally_checked')) {
                li.addClass("currently_checked");
                if (!isChecked) {
                    // this will trigger the checkbox handler to add/remove from the section list
                    currCheckbox.trigger('click');
                }
            } else {
                if (isChecked) {
                    currCheckbox.trigger('click');
                }
            }
        });

        //Disable submit button
        var submitButton = $('#continue-button');
        submitButton.attr("disabled", true);
        submitButton.attr("aria-disabled", true);
    });

    /* Continue Button, submits the form */
    $('#main').on('submit', function(event) {
        var submitButton = $('button:focus');

        handleLoading(submitButton);

        var sectionList = createJSON($('.sectionsList li'));

        var form = event.target;

        $('<input />').attr('type', 'hidden')
                .attr('name', "sectionList")
                .attr('value', JSON.stringify(sectionList))
                .appendTo(form);

        return true;
    });

    // this will prevent forms from submitting twice
    $('form').preventDoubleSubmission();

    $(function(){
        if ($("#alert").length) {
            $("#alert").focus();
        }
        if ($("#focusText").length) {
            $("#focusText").focus();
        }
    });

    $('#addTerm').on('change', function() {
        var obj = $(this);
        var urlBase = obj.data('urlbase');
        var termId = obj.val();
        var sectionList = createJSON($('.sectionsList li'));
        var jsonSectionList = JSON.stringify(sectionList);

        var collapsedTermsList = getCollapsedTermsString();

        // load the new term data
        $("#dataDiv").load(urlBase + termId, {sectionList: jsonSectionList, collapsedTerms: collapsedTermsList},
        function(response, status, xhr) {
            if (xhr.status == 403) {
                window.location.replace("error");
            }

            loadUnavailableSections();

        });
        // remove the term option from the map since there won't be a need to select it again
        $("#addTerm option[value=" + termId + "]").remove();
        $("#loading").show();

        // Call Canvas's message listener to resize the iframe since the loading icon
        // pushes the content down
        parent.postMessage(JSON.stringify({subject: 'lti.frameResize', height: $(document).height()}), '*');

        $("#addTerm").attr("disabled", true);
        $("#addTerm").attr("aria-disabled", true);
    });

    $('#cancel-button,#edit-button,#submit-button').on('click', function(){
        var actionButton = $(this);

        // when we disable buttons, their values are not submitted. So we need to set the hidden input
        // to the value of the button clicked
        var submitValue = actionButton.val();
        $('#submitValue').val(submitValue);

        handleLoading(actionButton);

        $("#confirmation_form").trigger('submit');
    });

    $(document).ajaxComplete(function(event, xhr, settings) {
        var unavailableSectionsUrl = $('#unavailable-sections-load').data('urlbase');

        if (settings.url !== unavailableSectionsUrl) {
           checkboxEventRegistration();
           modalButtonToggle();
           $("#loading").hide();
           $("#addTerm").attr("disabled", false);
           $("#addTerm").attr("aria-disabled", false);

           // Canvas has a message listener to resize the iframe
           parent.postMessage(JSON.stringify({subject: 'lti.frameResize', height: $(document).height()}), '*');
        }

        // move focus to the newly added term
        var focusElement = $(".newTerm").first();
        if (focusElement) {
            focusElement.focus();
        }
    });
});

function handleLoading(actionButton) {
    actionButton.attr({'aria-busy': 'true'});
    actionButton.addClass("rvt-button--loading");

    var spinner = actionButton.find(".rvt-loader").first();
    if (spinner) {
        spinner.removeClass("rvt-display-none");
    }

    var SRText = actionButton.find(".loading-text");
    if (SRText) {
        SRText.removeClass("rvt-display-none");
    }

    $(".rvt-button").attr("disabled", "true");
}

/**
 *
 **/
function createJSON(obj) {
    jsonObj = [];
    $(obj).each(function() {
        //Should be a bunch of li elements
        var li = $(this);
        var id = li.data("sectionid");
        var name = li.data("sectionname");
        var term = li.data("termid");

        var item = {}
        item ["termId"] = term;
        item ["sectionId"] = id;
        item ["sectionName"] = name;
        item ["originallyChecked"] = li.hasClass('originally_checked');
        item ["currentlyChecked"] = li.hasClass('currently_checked');
        item ["displayCrosslistedElsewhereWarning"] = li.hasClass('crosslisted_elsewhere')

        jsonObj.push(item);
    });

    return jsonObj;
}

function getCollapsedTermsString(obj) {
    collapsedTerms = [];

    var obj = $('div.collapse:not(.in)')
    $(obj).each(function() {
        //Should be the collapsed divs. The id of the div is the term id.
        var term_id = $(this).attr("id");
        collapsedTerms.push(term_id);
    });

    return collapsedTerms.join();
}

function getCheckboxValues() {
    var checkboxValues = $.map($('input:checkbox:checked'), function(e, i) {
        return e.value;
    });
    return checkboxValues;
}

// jQuery plugin to prevent double submission of forms
jQuery.fn.preventDoubleSubmission = function() {
    $(this).on('submit',function(e){
        var $form = $(this);

        if ($form.data('submitted') === true) {
            // Previously submitted - don't submit again
            e.preventDefault();
        } else {
            // Mark it so that the next submit can be ignored
            $form.data('submitted', true);
            var buttons = $('button[type="submit"]');
            $(buttons).each(function() {
                $(this).css('opacity', '0.6');
            });
        }
    });

    // Keep chainability
    return this;
};

function checkboxEventRegistration() {
    $('.sectionsList :checkbox').on('click', function(event) {

        var currentBox = $(this);
        var li = currentBox.parent().parent();

        if (currentBox.is(":checked")) {
            var newLi = $("<li>", {
                    id: 'summary_' + currentBox.attr('id'),
                    class: 'sectionHighlight',
                    text: currentBox.val()
                })
                    .data('sectionid', currentBox.attr('id'))
                    .data('sectionname', currentBox.val());
            if (li.hasClass('crosslisted_elsewhere')) {
                newLi.append($($("label[for='" + currentBox.attr('id') + "']").children()[1]).clone(false));
            }

            $('.summaryList').append(newLi);
            $('.summaryList #summaryNone').remove();

            li.addClass("currently_checked");
        } else {
            $('.summaryList #summary_'+currentBox.attr('id')).remove();
            if ($('.summaryList li').length == 0) {
                $('.summaryList').append(summaryNone);
            }

            li.removeClass("currently_checked");
        }

        var updatedCheckedValue = getCheckboxValues();
        var submitButton = $('#continue-button');
        if ($(checkedValue).not(updatedCheckedValue).length === 0 && $(updatedCheckedValue).not(checkedValue).length === 0)
        {
            submitButton.attr("disabled", true);
            submitButton.attr("aria-disabled", true);
        }
        else
        {
            submitButton.attr("disabled", false);
            submitButton.removeAttr("aria-disabled");
        }
        currentBox.focus();
    });
}

// This will scroll back to the top after each page submission
$(window).on('unload', function(){
    // Canvas has a message listener to scroll to the top of the iframe
    parent.postMessage(JSON.stringify({subject: 'lti.scrollToTop'}), '*');
});

function modalButtonToggle() {
    $('#impersonateDropdown').change('click', function(event) {
        var modalSubmit = $('#modal-submit');
        if (!$('#impersonateDropdown').val()) {
            modalSubmit.attr("disabled", true);
            modalSubmit.attr("aria-disabled", true);
        } else {
            modalSubmit.attr("disabled", false);
            modalSubmit.attr("aria-disabled", false);
        }
    });
}

function loadUnavailableSections() {
    var loadDiv = $('#unavailable-sections-load');
    var urlBase = loadDiv.data('urlbase');

    var token = $('#_csrf').attr('content');
    var header = $('#_csrf_header').attr('content');
    $(document).ajaxSend(function(e,xhr,options) {
       xhr.setRequestHeader(header, token);
    });

    loadDiv.empty();

    var displayedTerms = [];

    var activeTerm = $('#active-term');
    var olderTerms = $('button.toggleoverride');

    displayedTerms.push(activeTerm.data('active-term-id'));

    olderTerms.each(function() {
                        displayedTerms.push($(this).attr('aria-controls'));
    });

   var joinedTerms = displayedTerms.join();

    // Call Canvas's message listener to resize the iframe since the loading icon
    // pushes the content down
    parent.postMessage(JSON.stringify({subject: 'lti.frameResize', height: $(document).height()}), '*');

    // load the new unavailable sections
    loadDiv.load(urlBase, {joinedTerms: joinedTerms},
        function(response, status, xhr) {
            if (xhr.status == 403) {
                window.location.replace("error");
            }
        });
}

// use this to force code to wait for a certain element to be rendered
function waitForElm(selector) {
    return new Promise(resolve => {
        if (document.querySelector(selector)) {
            return resolve(document.querySelector(selector));
        }

        const observer = new MutationObserver(mutations => {
            if (document.querySelector(selector)) {
                resolve(document.querySelector(selector));
                observer.disconnect();
            }
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    });
}