import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { frankframeworkMemberGuard } from './frankframework-member.guard';
import { AuthService, User } from '../services/auth.service';

describe('frankframeworkMemberGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const mockUser: User = {
    githubId: 123,
    username: 'testuser',
    avatarUrl: 'https://example.com/avatar.jpg',
    isFrankFrameworkMember: true,
  };

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', [], {
      currentUser: signal<User | null>(null),
    });
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should allow access when user is a FrankFramework member', () => {
    authService.currentUser = signal<User | null>(mockUser);

    const result = TestBed.runInInjectionContext(() => frankframeworkMemberGuard({} as any, {} as any));

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should deny access and redirect when user is not a FrankFramework member', () => {
    const nonMemberUser: User = { ...mockUser, isFrankFrameworkMember: false };
    authService.currentUser = signal<User | null>(nonMemberUser);

    const result = TestBed.runInInjectionContext(() => frankframeworkMemberGuard({} as any, {} as any));

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should deny access and redirect when user is not logged in', () => {
    authService.currentUser = signal<User | null>(null);

    const result = TestBed.runInInjectionContext(() => frankframeworkMemberGuard({} as any, {} as any));

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('should deny access and redirect when user object is undefined', () => {
    authService.currentUser = signal<User | null>(null);

    const result = TestBed.runInInjectionContext(() => frankframeworkMemberGuard({} as any, {} as any));

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
