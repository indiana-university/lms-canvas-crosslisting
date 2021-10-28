var summaryNone = "<li id='summaryNone'>None</li>";
var summary = $('.summaryList').clone(true);
var checkedValue;

$(document).ready(function(){
    loadUnavailableSections();

    checkedValue = getCheckboxValues();

    checkboxEventRegistration();
    modalButtonToggle();

    // Canvas has a message listener to resize the iframe
    parent.postMessage(JSON.stringify({subject: 'lti.frameResize', height: $(document).height()}), '*');

    /* Reset Button */
    $('#reset-button').on('click', function(){
        //Reset summary area
        $('.summaryList').replaceWith(summary);
        summary = $('.summaryList').clone(true);

        //Reset section area
        $('.sectionsList li').each(function() {
            var li = $(this);
            li.removeClass("currently_checked");
            if (li.hasClass('originally_checked')) {
                li.addClass("currently_checked");
                li.find('input[type=checkbox]').attr('checked');
            } else {
                li.find('input[type=checkbox]').removeAttr('checked');
            }
        });

        //Disable submit button
        var submitButton = $('#continue-button');
        submitButton.attr("disabled", true);
        submitButton.attr("aria-disabled", true);
    });

    /* Continue Button, submits the form */
    $('#main').submit(function(event) {
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

            // move focus to the newly added section
            $("button[aria-controls=" + termId + "]").focus();

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
        $("#loading").show();
    });

    // this controls the toggle dropdowns
    $(document).on('click', '.toggleGroup', function() {
        
        $(this).parent().find('.toggler:first').slideToggle("slow");
        
        if ($(this).find("use:first").attr("xlink:href").includes("rvt-icon-chevron-down")) {
            $(this).find("button:first").attr("aria-expanded","false");
            $(this).find("use:first").attr("xlink:href", function(index, old) {
                return old.replace("rvt-icon-chevron-down", "rvt-icon-chevron-right");
            });
        } else {
            $(this).find("button:first").attr("aria-expanded","true");
            $(this).find("use:first").attr("xlink:href", function(index, old) {
                return old.replace("rvt-icon-chevron-right", "rvt-icon-chevron-down");
            });
        }
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
    });
});

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
    $('.sectionsList :checkbox').change('click', function(event) {
        event.stopPropagation();
        event.preventDefault();

        var currentBox = $(this);
        var li = currentBox.parent();

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

    loadDiv.empty();

    $("#unavailable-loading").show();

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

            $("#unavailable-loading").hide();
        });
}