import { Component, OnInit, signal } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { User } from '../../core/user/user.model';
import { UserService } from '../../core/user/user.service';
import { UserFormDialog } from '../../layout/user-form-dialog/user-form-dialog';
import { ConfirmDialog } from '../../layout/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-user-list',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatPaginatorModule, MatDialogModule],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss',
})
export class UserList implements OnInit {
  protected readonly displayedColumns = ['login', 'firstName', 'lastName', 'actions'];
  protected readonly users = signal<User[]>([]);
  protected readonly totalElements = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly pageIndex = signal(0);

  constructor(
    private readonly userService: UserService,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadUsers();
  }

  protected openCreateDialog(): void {
    const dialogRef = this.dialog.open(UserFormDialog, { width: '480px' });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadUsers();
      }
    });
  }

  protected openEditDialog(user: User): void {
    const dialogRef = this.dialog.open(UserFormDialog, {
      width: '480px',
      data: user,
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadUsers();
      }
    });
  }

  protected deleteUser(user: User): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      data: { title: 'Delete User', message: `Delete user "${user.login}"?` },
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.userService.deleteUser(user.id).subscribe(() => this.loadUsers());
      }
    });
  }

  private loadUsers(): void {
    this.userService.getUsers(this.pageIndex(), this.pageSize()).subscribe((result) => {
      this.users.set(result.content);
      this.totalElements.set(result.totalElements);
    });
  }
}
