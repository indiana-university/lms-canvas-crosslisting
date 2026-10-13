/*-
 * #%L
 * lms-lti-crosslist
 * %%
 * Copyright (C) 2015 - 2025 Indiana University
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
const summaryNone = "<li id='summaryNone'>None</li>";
let checkedValue;

$(document).ready(function(){
    // this section sometimes processes before it actually exists
    // put it in this waitForElm method to make it wait until it exists in the dom
    waitForElm('#unavailable-sections-load').then((elm) => {
        loadUnavailableSections();
    });

    checkedValue = getCheckboxValues();

    checkboxEventRegistration();
    modalButtonToggle();

    // Canvas has a message listener to resize the iframe. Re-send the height whenever the page content changes size
    // (ajax-loaded sections, toggled terms, loaders, late-loading fonts, etc.)
    const resizeObserver = new ResizeObserver(() => resizeCanvasFrame());
    $('body').children().each(function() {
        resizeObserver.observe(this);
    });

    /* Reset Button */
    $('#reset-button').on('click', function(){

        //Reset section area
        $('.sectionsList li').each(function() {
            let li = $(this);
            let currCheckbox = li.find('input[type=checkbox]');
            let isChecked = currCheckbox.is(":checked");
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
        let submitButton = $('#continue-button');
        submitButton.attr("disabled", true);
        submitButton.attr("aria-disabled", true);

        // move focus back to the reset button and announce reset
        $(this).focus();
        $('#resetMsg').text('Changes reset.');
    });

    /* Continue Button, submits the form */
    $('#main').on('submit', function(event) {
        let submitButton = $('button:focus');

        handleLoading(submitButton);

        let sectionList = createJSON($('.sectionsList li'));

        let form = event.target;

        $('<input />').attr('type', 'hidden')
                .attr('name', "sectionList")
                .attr('value', JSON.stringify(sectionList))
                .appendTo(form);

        return true;
    });

    /* Impersonation modal submit */
    $('#impersonation-form').on('submit', function() {
        handleLoading($('#modal-submit'));
        return true;
    });

    /* Exit impersonation submit */
    $('#exit-impersonation-form').on('submit', function() {
        handleLoading($('#exit-impersonation-button'));
        return true;
    });

    // this will prevent forms from submitting twice
    $('form').preventDoubleSubmission();

    $('#addTerm').on('change', function() {
        let obj = $(this);
        let urlBase = obj.data('urlbase');
        let termId = obj.val();
        let sectionList = createJSON($('.sectionsList li'));
        let jsonSectionList = JSON.stringify(sectionList);

        lockSectionCheckboxInteraction(true);

        let collapsedTermsList = getCollapsedTermsString();

        // load the new term data
        $("#dataDiv").load(urlBase + termId, {sectionList: jsonSectionList, collapsedTerms: collapsedTermsList},
        function(response, status, xhr) {
            if (xhr.status === 403) {
                window.location.replace("error");
            }

            loadUnavailableSections();

        });
        // remove the term option from the map since there won't be a need to select it again
        $("#addTerm option[value=" + termId + "]").remove();
        $("#loading").show();

        $("#addTerm").attr("disabled", true)
            .attr("aria-disabled", true);
    });

    $('#cancel-button,#edit-button,#submit-button').on('click', function(){
        let actionButton = $(this);

        // when we disable buttons, their values are not submitted. So we need to set the hidden input
        // to the value of the button clicked
        let submitValue = actionButton.val();
        $('#submitValue').val(submitValue);

        handleLoading(actionButton);

        $("#confirmation_form").trigger('submit');
    });

    $(document).ajaxComplete(function(event, xhr, settings) {
        let unavailableSectionsUrl = $('#unavailable-sections-load').data('urlbase');

        if (settings.url !== unavailableSectionsUrl) {
           checkboxEventRegistration();
           modalButtonToggle();
           lockSectionCheckboxInteraction(false);
           $("#loading").hide();
           $("#addTerm").attr("disabled", false)
               .attr("aria-disabled", false);
        }

        // move focus to the newly added term
        let focusElement = $(".newTerm").first();
        if (focusElement) {
            focusElement.focus();
        }
    });
});

function handleLoading(actionButton) {
    actionButton.attr({'aria-busy': 'true'});
    actionButton.addClass("rvt-button--loading");

    let spinner = actionButton.find(".rvt-loader").first();
    if (spinner) {
        spinner.removeClass("rvt-display-none");
    }

    let SRText = actionButton.find(".loading-text");
    if (SRText) {
        SRText.removeClass("rvt-display-none");
    }

    $(".rvt-button").attr("disabled", "true");
}

function lockSectionCheckboxInteraction(shouldLock) {
    let checkboxes = $('.sectionsList :checkbox');
    checkboxes.prop('disabled', shouldLock);

    if (shouldLock) {
        checkboxes.attr('aria-disabled', true);
    } else {
        checkboxes.removeAttr('aria-disabled');
    }
}

/**
 *
 **/
function createJSON(obj) {
    let jsonObj = [];
    $(obj).each(function() {
        //Should be a bunch of li elements
        let li = $(this);
        let id = li.data("sectionid");
        let name = li.data("sectionname");
        let term = li.data("termid");

        let item = {}
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

function getCollapsedTermsString() {
    let collapsedTerms = [];

    let obj = $('div.collapse:not(.in)')
    $(obj).each(function() {
        //Should be the collapsed divs. The id of the div is the term id.
        let term_id = $(this).attr("id");
        collapsedTerms.push(term_id);
    });

    return collapsedTerms.join();
}

function getCheckboxValues() {
    let checkboxValues = $.map($('input:checkbox:checked'), function(e, i) {
        return e.value;
    });
    return checkboxValues;
}

// jQuery plugin to prevent double submission of forms
jQuery.fn.preventDoubleSubmission = function() {
    $(this).on('submit',function(e){
        let $form = $(this);

        if ($form.data('submitted') === true) {
            // Previously submitted - don't submit again
            e.preventDefault();
        } else {
            // Mark it so that the next submit can be ignored
            $form.data('submitted', true);
            let buttons = $('button[type="submit"]');
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
        // make sure the "reset" msg is cleared out since there is a new change
        $('#resetMsg').text('');

        let currentBox = $(this);
        let li = currentBox.parent().parent();

        if (currentBox.is(":checked")) {
            let newLi = $("<li>", {
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
            if ($('.summaryList li').length === 0) {
                $('.summaryList').append(summaryNone);
            }

            li.removeClass("currently_checked");
        }

        let updatedCheckedValue = getCheckboxValues();
        let submitButton = $('#continue-button');
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
        let modalSubmit = $('#modal-submit');
        if (!$('#impersonateDropdown').val()) {
            modalSubmit.attr("disabled", true);
            modalSubmit.attr("aria-disabled", true);
        } else {
            modalSubmit.attr("disabled", false);
            modalSubmit.attr("aria-disabled", false);
        }
    });
}

/**
 * Ask Canvas to resize the tool's iframe to fit the content.
 * Note: Rivet sets both html and body to height:100%, so the document never reports less than the iframe's current
 * height. The iframe can grow to fit new content but will not shrink when there is less of it.
 */
function resizeCanvasFrame() {
    parent.postMessage(JSON.stringify({subject: 'lti.frameResize', height: $(document).height()}), '*');
}

function loadUnavailableSections() {
    let loadDiv = $('#unavailable-sections-load');
    let urlBase = loadDiv.data('urlbase');

    let token = $('#_csrf').attr('content');
    let header = $('#_csrf_header').attr('content');
    $(document).ajaxSend(function(e,xhr,options) {
       xhr.setRequestHeader(header, token);
    });

    loadDiv.empty();

    let displayedTerms = [];

    let activeTerm = $('#active-term');
    let olderTerms = $('button.toggleoverride');

    displayedTerms.push(activeTerm.data('active-term-id'));

    olderTerms.each(function() {
                        displayedTerms.push($(this).attr('aria-controls'));
    });

    let joinedTerms = displayedTerms.join();

    // load the new unavailable sections
    loadDiv.load(urlBase, {joinedTerms: joinedTerms},
        function(response, status, xhr) {
            if (xhr.status === 403) {
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
