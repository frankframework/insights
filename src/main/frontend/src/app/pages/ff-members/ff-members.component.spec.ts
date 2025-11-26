import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { FfMembersComponent } from './ff-members.component';
import { AuthService, User } from '../../services/auth.service';

describe('FfMembersComponent', () => {
  let component: FfMembersComponent;
  let fixture: ComponentFixture<FfMembersComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://example.com/avatar.jpg',
    isFrankFrameworkMember: true,
  };

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', [], {
      currentUser: signal<User | null>(mockUser),
    });

    await TestBed.configureTestingModule({
      imports: [FfMembersComponent],
      providers: [{ provide: AuthService, useValue: mockAuthService }],
    }).compileComponents();

    fixture = TestBed.createComponent(FfMembersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the username in the welcome message', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const welcomeMessage = compiled.querySelector('.welcome-message strong');

    expect(welcomeMessage?.textContent).toContain('testuser');
  });

  it('should display the page heading', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const heading = compiled.querySelector('h1');

    expect(heading?.textContent).toBe('FrankFramework Members Area');
  });

  it('should display business value management section', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const sections = compiled.querySelectorAll('.info-card h2');

    expect(sections[0]?.textContent).toBe('Business Value Management');
  });

  it('should display permissions section', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const sections = compiled.querySelectorAll('.info-card h2');

    expect(sections[1]?.textContent).toBe('Your Permissions');
  });
});
