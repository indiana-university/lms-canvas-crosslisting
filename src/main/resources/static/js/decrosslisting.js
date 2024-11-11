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

jQuery(document).ready(function() {
    var resultsMessage = document.getElementById('results-message');

    if (resultsMessage != null) {
        resultsMessage.focus();
    }

    $("#checkbox-select-all").click(function() {
        $('input:checkbox').not(this).prop('checked', this.checked);
    });

    checkboxCounter();

    $('[aria-invalid="true"]').first().focus();
});

function checkboxCounter() {
    // Get all the selected checkboxes, except the "select-all" one up in the table header
    var $checkboxes = $('#decrosslist-form input[type="checkbox"]');

    // update the count
    $checkboxes.change(function() {
        var countCheckedCheckboxes = $checkboxes.not("#checkbox-select-all").filter(':checked').length;
        $('#sections-selected').text(countCheckedCheckboxes + ' selected');
    });
}

function submitSisIdForm(button) {
    buttonLoading(button);
    document.getElementById('sisIdForm').submit();
}

function validateCheckboxForm(button) {
    // Get all the selected checkboxes, except the "select-all" one up in the table header
    var checkboxes = $('#decrosslist-form input[type="checkbox"]').not("#checkbox-select-all").filter(':checked').length;

    if (checkboxes > 0) {
        buttonLoading(button);
        document.getElementById('decrosslist-form').submit();
    } else {
        var checkboxValidationMessage = document.getElementById('checkbox-validation-message');
        checkboxValidationMessage.classList.remove('rvt-display-none');

        let firstCheckbox = $('#decrosslist-form input[type="checkbox"]').not("#checkbox-select-all").first();
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
    var buttonsToDisable = document.getElementsByTagName('button');
    for(var i = 0; i < buttonsToDisable.length; i++) {
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