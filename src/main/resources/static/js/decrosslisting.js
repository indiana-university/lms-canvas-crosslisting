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

jQuery(document).ready(function() {
    // Move focus to the most relevant thing on the page so screen readers announce it after each page load.
    // The status alert is rendered with the page, so its role="alert" alone won't reliably be announced.
    // Otherwise (first load, or a failed search) focus the search input so the user can start typing.
    let focusTarget = document.getElementById('alert')
        || document.getElementById('results-message')
        || document.getElementById('sisid-search');

    if (focusTarget != null) {
        focusTarget.focus();
    }

    $("#select-all-button").click(function() {
        $('input:checkbox').prop('checked', true);
        updateCounter();
    });

    $("#deselect-all-button").click(function() {
        $('input:checkbox').prop('checked', false);
        updateCounter();
    });

    checkboxCounter();
});

function checkboxCounter() {
    // Get all the selected checkboxes, except the "select-all" one up in the table header
    let $checkboxes = $('#decrosslist-form input[type="checkbox"]');

    // update the count
    $checkboxes.change(function() {
        let countCheckedCheckboxes = $checkboxes.filter(':checked').length;
        $('#sections-selected').text(countCheckedCheckboxes + ' selected');
    });
}

function updateCounter() {
    let $checkboxes = $('#decrosslist-form input[type="checkbox"]');
    let countCheckedCheckboxes = $checkboxes.filter(':checked').length;
    $('#sections-selected').text(countCheckedCheckboxes + ' selected');
}

function submitSisIdForm(button) {
    let searchInput = document.getElementById('sisid-search');
    if (searchInput.value.trim() === '') {
        let validationText = document.getElementById('sisid-error-text');
        validationText.innerText = 'SIS Section ID is required.';

        let validationMessage = document.getElementById('sisid-error-message');
        validationMessage.classList.remove('rvt-display-none');

        searchInput.setAttribute('aria-invalid', true);
        searchInput.setAttribute('aria-describedby', 'sisid-error-text sisid-search-hint sisid-search-description');
        searchInput.focus();

        return false;
    } else {
        buttonLoading(button);
        document.getElementById('sisIdForm').submit();
    }
}

function validateCheckboxForm(button) {
    // Get all the checkboxes, except the "select-all" one up in the table header
    const allCheckboxes = $('#decrosslist-form input[type="checkbox"]').not("#checkbox-select-all");
    const checkedCount = allCheckboxes.filter(':checked').length;

    if (checkedCount > 0) {
        buttonLoading(button);
        document.getElementById('decrosslist-form').submit();
    } else {
        const checkboxValidationMessage = document.getElementById('checkbox-validation-message');
        checkboxValidationMessage.classList.remove('rvt-display-none');

        const firstCheckbox = allCheckboxes.first();
        firstCheckbox.attr('aria-invalid', true);
        firstCheckbox.attr('aria-describedby', 'checkbox-validation-message');
        firstCheckbox.focus();

        return false;
    }
}

function buttonLoading(button) {
    if (button.dataset.action != null) {
        document.getElementById("submitValue").value = button.dataset.action;
    }
    button.setAttribute("aria-busy", true);
    let buttonsToDisable = document.getElementsByTagName('button');
    for(let i = 0; i < buttonsToDisable.length; i++) {
        buttonsToDisable[i].disabled = true;
    }
    button.classList.add("rvt-button--loading");

    let spinners = button.getElementsByClassName('rvt-loader');
    if (spinners && spinners.length > 0) {
        spinners[0].classList.remove("rvt-display-none");
    }

    let srText = button.getElementsByClassName('sr-loader-text');
    if (srText && srText.length > 0) {
        srText[0].classList.remove("rvt-display-none");
    }
}
