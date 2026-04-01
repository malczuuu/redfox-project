import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { User } from '../../core/user/user.model';
import { UserService } from '../../core/user/user.service';

@Component({
  selector: 'app-user-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './user-form-dialog.html',
  styleUrl: './user-form-dialog.scss',
})
export class UserFormDialog implements OnInit {
  protected form!: FormGroup;
  protected isEdit = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly userService: UserService,
    private readonly dialogRef: MatDialogRef<UserFormDialog>,
    @Inject(MAT_DIALOG_DATA) public data: User | null,
  ) {}

  ngOnInit(): void {
    this.isEdit = !!this.data;
    this.form = this.fb.group({
      login: [{ value: this.data?.login ?? '', disabled: this.isEdit }, Validators.required],
      passhash: [{ value: '', disabled: this.isEdit }, this.isEdit ? [] : Validators.required],
      firstName: [this.data?.firstName ?? '', Validators.required],
      lastName: [this.data?.lastName ?? '', Validators.required],
    });
  }

  protected onSubmit(): void {
    if (!this.form.valid) {
      return;
    }

    if (this.isEdit && this.data) {
      this.userService
        .updateUser(this.data.id, {
          firstName: this.form.value.firstName,
          lastName: this.form.value.lastName,
          version: this.data.version!,
        })
        .subscribe(() => this.dialogRef.close(true));
    } else {
      this.userService
        .createUser(this.form.getRawValue())
        .subscribe(() => this.dialogRef.close(true));
    }
  }
}
